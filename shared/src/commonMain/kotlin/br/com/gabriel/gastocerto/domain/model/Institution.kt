package br.com.gabriel.gastocerto.domain.model

/** Bank or brokerage that issues payment methods (Nubank, Inter, Wise, Clear…) – RF02. Equality by [id] (D2). */
class Institution(
    val name: String,
    val id: String = newId(),
) {
    override fun equals(other: Any?): Boolean = other is Institution && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Institution($name)"
}
