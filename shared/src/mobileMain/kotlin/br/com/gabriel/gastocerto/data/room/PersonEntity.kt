package br.com.gabriel.gastocerto.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.gabriel.gastocerto.domain.model.Person
import br.com.gabriel.gastocerto.domain.model.Role

/** Table row for [Person]. The domain never sees this class (RNF07): the mappers below convert both ways. */
@Entity(tableName = "person")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** [Role] name, so reordering the enum does not corrupt stored data. */
    val role: String,
)

fun Person.toEntity(): PersonEntity = PersonEntity(id = id, name = name, role = role.name)

fun PersonEntity.toDomain(): Person = Person(name = name, role = Role.valueOf(role), id = id)
