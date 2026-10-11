package com.shubham.cards.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A saved card. Only non-identifying metadata (title, tech summary) is stored in
 * the clear so lists can render fast. The full property set — which includes the
 * UID and any payloads — is stored in [encryptedDetails], encrypted via
 * [CryptoManager] with a Keystore key.
 */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val title: String,
    val techSummary: String,
    /** Base64(iv||ciphertext) of the JSON property list. Never plaintext. */
    val encryptedDetails: String,
)

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}

@Database(entities = [CardEntity::class], version = 1, exportSchema = false)
abstract class CardsDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile private var instance: CardsDatabase? = null

        fun get(context: Context): CardsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CardsDatabase::class.java,
                    "cards.db",
                ).build().also { instance = it }
            }
    }
}
