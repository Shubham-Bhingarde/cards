package com.shubham.cards.data

import android.content.Context
import com.shubham.cards.log.AppLogger
import com.shubham.cards.nfc.CardInspection
import com.shubham.cards.nfc.CardProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** A saved card with its details already decrypted for display. */
data class SavedCard(
    val id: Long,
    val createdAt: Long,
    val title: String,
    val techSummary: String,
    val properties: List<CardProperty>,
)

/**
 * Mediates between the NFC layer and the encrypted database. Encryption/decryption
 * of the detail blob happens here so the rest of the app never touches ciphertext.
 */
class CardRepository(context: Context) {

    private val dao = CardsDatabase.get(context).cardDao()
    private val tag = "CardRepository"

    val cards: Flow<List<SavedCard>> =
        dao.observeAll().map { list -> list.map { it.toSavedCardSafe() } }

    suspend fun save(inspection: CardInspection): Long {
        val json = JSONArray().apply {
            inspection.properties.forEach { p ->
                put(JSONObject().apply {
                    put("label", p.label)
                    put("value", p.value)
                    put("sensitive", p.sensitive)
                })
            }
        }
        val entity = CardEntity(
            createdAt = System.currentTimeMillis(),
            title = inspection.displayTitle,
            techSummary = inspection.technologies.joinToString(", "),
            encryptedDetails = CryptoManager.encrypt(json.toString()),
        )
        val id = dao.insert(entity)
        AppLogger.i(tag, "Saved card id=$id kind='${inspection.displayTitle}' (details encrypted)")
        return id
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
        AppLogger.i(tag, "Deleted card id=$id")
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        AppLogger.i(tag, "Deleted all saved cards")
    }

    private fun CardEntity.toSavedCardSafe(): SavedCard {
        val props = try {
            val arr = JSONArray(CryptoManager.decrypt(encryptedDetails))
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        CardProperty(
                            label = o.getString("label"),
                            value = o.getString("value"),
                            sensitive = o.optBoolean("sensitive", false),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to decrypt card id=$id", e)
            listOf(CardProperty("Error", "Could not decrypt stored details"))
        }
        return SavedCard(id, createdAt, title, techSummary, props)
    }
}
