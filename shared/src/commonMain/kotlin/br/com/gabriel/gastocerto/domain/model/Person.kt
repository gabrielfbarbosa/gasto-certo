package br.com.gabriel.gastocerto.domain.model

/** Role of a person in the household expense tracking. */
enum class Role {
    /** Whoever operates the app. */
    USER,

    /** Owner of a card/account whose expenses count toward the household total (e.g. spouse). */
    HOLDER,

    /** Person the user pays expenses for, who will pay them back (e.g. father) – RN03. */
    THIRD_PARTY,
}

/** A registered person: user, holder or third party (RF01, RN10). Equality by [id] (D2). */
class Person(
    val name: String,
    val role: Role,
    val id: String = newId(),
) {
    override fun equals(other: Any?): Boolean = other is Person && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Person($name, $role)"
}
