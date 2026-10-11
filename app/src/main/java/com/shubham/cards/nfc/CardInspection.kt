package com.shubham.cards.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import java.io.IOException

/**
 * A single readable property of a tapped card (label + value), e.g. "SAK" -> "0x08".
 * [sensitive] marks values that identify the physical card (UID etc.) so the logger
 * can redact them by default.
 */
data class CardProperty(
    val label: String,
    val value: String,
    val sensitive: Boolean = false,
)

/**
 * The result of inspecting one tapped card. This is a *read-only* identification:
 * it records what technology the card uses and whatever can be read WITHOUT
 * authenticating to protected memory. It deliberately does NOT attempt to crack
 * keys or dump protected sectors — this is a diagnostic/identifier, not a cloner.
 */
data class CardInspection(
    val uidHex: String,
    val technologies: List<String>,
    /** Best-effort human label of the card family, e.g. "MIFARE Classic 1K". */
    val cardKind: String,
    /** Whether this card exposes NDEF (a standard, openly readable data format). */
    val hasNdef: Boolean,
    val properties: List<CardProperty>,
    /** Non-fatal notes gathered while reading (e.g. "NDEF present but empty"). */
    val notes: List<String>,
) {
    /** A short title for lists/cards. */
    val displayTitle: String get() = cardKind.ifBlank { technologies.firstOrNull() ?: "Unknown card" }
}

/**
 * Inspects a tapped [Tag] and returns a structured, read-only report.
 *
 * All tech I/O is wrapped in try/catch with the connection always closed, so a
 * card pulled away mid-read never crashes the app — it just yields fewer
 * properties plus a note.
 */
object CardInspector {

    private val TECH_LABELS = mapOf(
        NfcA::class.java.name to "NFC-A (ISO 14443-3A)",
        NfcB::class.java.name to "NFC-B (ISO 14443-3B)",
        NfcF::class.java.name to "NFC-F (FeliCa / JIS 6319-4)",
        NfcV::class.java.name to "NFC-V (ISO 15693)",
        IsoDep::class.java.name to "ISO-DEP (ISO 14443-4)",
        MifareClassic::class.java.name to "MIFARE Classic",
        MifareUltralight::class.java.name to "MIFARE Ultralight",
        Ndef::class.java.name to "NDEF",
        "android.nfc.tech.NdefFormatable" to "NDEF Formatable",
        "android.nfc.tech.NfcBarcode" to "NFC Barcode",
    )

    fun inspect(tag: Tag): CardInspection {
        val techList = tag.techList.toList()
        val friendlyTechs = techList.map { TECH_LABELS[it] ?: it.substringAfterLast('.') }
        val uid = tag.id ?: ByteArray(0)
        val props = mutableListOf<CardProperty>()
        val notes = mutableListOf<String>()

        props += CardProperty("UID", uid.toHex(), sensitive = true)
        props += CardProperty("UID length", "${uid.size} bytes")

        var cardKind = ""

        // --- Low-level 3A/3B/F/V identifiers (safe, no auth) ---
        NfcA.get(tag)?.let { a ->
            props += CardProperty("ATQA", a.atqa.toHex())
            props += CardProperty("SAK", "0x%02X".format(a.sak))
        }
        NfcB.get(tag)?.let { b ->
            props += CardProperty("Application data", b.applicationData.toHex())
            props += CardProperty("Protocol info", b.protocolInfo.toHex())
        }
        NfcF.get(tag)?.let { f ->
            props += CardProperty("Manufacturer", f.manufacturer.toHex())
            props += CardProperty("System code", f.systemCode.toHex())
        }
        NfcV.get(tag)?.let { v ->
            props += CardProperty("DSF ID", "0x%02X".format(v.dsfId))
            props += CardProperty("Response flags", "0x%02X".format(v.responseFlags))
        }

        // --- ISO-DEP: read the historical/hi-layer bytes used to recognise smartcards ---
        IsoDep.get(tag)?.let { iso ->
            iso.historicalBytes?.let { props += CardProperty("Historical bytes (ATS)", it.toHex()) }
            iso.hiLayerResponse?.let { props += CardProperty("Hi-layer response", it.toHex()) }
        }

        // --- MIFARE Classic: report STRUCTURE ONLY. We do not authenticate or dump
        //     sectors; protected content requires keys we intentionally never attempt. ---
        MifareClassic.get(tag)?.let { mc ->
            cardKind = when (mc.type) {
                MifareClassic.TYPE_CLASSIC -> "MIFARE Classic"
                MifareClassic.TYPE_PLUS -> "MIFARE Plus"
                MifareClassic.TYPE_PRO -> "MIFARE Pro"
                else -> "MIFARE Classic (unknown subtype)"
            }
            val sizeK = mc.size / 1024.0
            cardKind += " ${trimK(sizeK)}"
            props += CardProperty("Size", "${mc.size} bytes")
            props += CardProperty("Sectors", mc.sectorCount.toString())
            props += CardProperty("Blocks", mc.blockCount.toString())
            notes += "MIFARE Classic memory is key-protected. This app reports the " +
                "card's structure only and does not read, dump, or crack protected sectors."
        }

        MifareUltralight.get(tag)?.let { mu ->
            cardKind = when (mu.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                else -> "MIFARE Ultralight (unknown subtype)"
            }
        }

        // --- NDEF: an open, standard data format meant to be read. Safe to read fully. ---
        var hasNdef = false
        Ndef.get(tag)?.let { ndef ->
            hasNdef = true
            if (cardKind.isBlank()) cardKind = "NDEF tag (${ndef.type})"
            props += CardProperty("NDEF type", ndef.type)
            props += CardProperty("NDEF max size", "${ndef.maxSize} bytes")
            props += CardProperty("NDEF writable", ndef.isWritable.toString())
            readNdef(ndef, props, notes)
        }

        if (cardKind.isBlank()) {
            cardKind = friendlyTechs.firstOrNull() ?: "Unknown card"
        }

        return CardInspection(
            uidHex = uid.toHex(),
            technologies = friendlyTechs,
            cardKind = cardKind,
            hasNdef = hasNdef,
            properties = props,
            notes = notes,
        )
    }

    private fun readNdef(
        ndef: Ndef,
        props: MutableList<CardProperty>,
        notes: MutableList<String>,
    ) {
        try {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            if (message == null) {
                notes += "NDEF present but empty."
                return
            }
            val records = message.records
            props += CardProperty("NDEF records", records.size.toString())
            records.forEachIndexed { i, record ->
                val tnf = record.tnf
                val type = String(record.type, Charsets.US_ASCII)
                props += CardProperty("Record #${i + 1} type", "TNF=$tnf, type=$type")
                // Payload content is treated as sensitive (may encode identity/URLs).
                props += CardProperty(
                    "Record #${i + 1} payload",
                    record.payload.toHex(),
                    sensitive = true,
                )
            }
        } catch (e: IOException) {
            notes += "Could not read NDEF (card moved too soon?): ${e.message}"
        } catch (e: Exception) {
            notes += "Unexpected error reading NDEF: ${e.message}"
        } finally {
            try {
                ndef.close()
            } catch (_: IOException) {
                // ignore – nothing we can do on close failure
            }
        }
    }

    private fun trimK(k: Double): String =
        if (k == k.toLong().toDouble()) "${k.toLong()}K" else "%.1fK".format(k)
}

/** Lowercase, space-separated hex. Empty array -> "(none)". */
fun ByteArray.toHex(): String =
    if (isEmpty()) "(none)" else joinToString(" ") { "%02X".format(it) }
