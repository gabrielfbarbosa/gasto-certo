package br.com.gabriel.gastocerto.domain.model

/** Banco ou corretora que emite meios de pagamento (Nubank, Inter, Wise, Clear…) – RF02. Igualdade por [id] (D2). */
class Instituicao(
    val nome: String,
    val id: String = novoId(),
) {
    override fun equals(other: Any?): Boolean = other is Instituicao && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Instituicao($nome)"
}
