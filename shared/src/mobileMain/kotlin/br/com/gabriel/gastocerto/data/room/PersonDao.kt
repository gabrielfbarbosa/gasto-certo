package br.com.gabriel.gastocerto.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PersonDao {
    @Upsert
    suspend fun upsert(person: PersonEntity)

    @Query("SELECT * FROM person ORDER BY name")
    suspend fun all(): List<PersonEntity>

    @Query("SELECT * FROM person WHERE id = :id")
    suspend fun findById(id: String): PersonEntity?
}
