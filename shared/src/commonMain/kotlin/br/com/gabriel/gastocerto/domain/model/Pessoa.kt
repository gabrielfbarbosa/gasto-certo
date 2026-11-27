package br.com.gabriel.gastocerto.domain.model

/** Papel de uma pessoa no controle de gastos da família. */
enum class Papel {
    /** Quem opera o app. */
    USUARIO,

    /** Dono de um cartão/conta cujos gastos entram no total da família (ex.: esposa). */
    TITULAR,

    /** Pessoa por quem o usuário paga despesas que serão devolvidas (ex.: pai) – RN03. */
    TERCEIRO,
}

/** Pessoa cadastrada: usuário, titular ou terceiro reembolsante (RF01, RN10). Igualdade por [id] (D2). */
class Pessoa(
    val nome: String,
    val papel: Papel,
    val id: String = novoId(),
) {
    override fun equals(other: Any?): Boolean = other is Pessoa && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Pessoa($nome, $papel)"
}
