package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.LocalDate

/**
 * RN03 – valor devolvido por um [terceiro]. Dá baixa no saldo a receber e **não é receita**.
 * [valor] > 0 (RN09); não pode passar do saldo ([SaldoAReceber.validarReembolso]). Igualdade por [id] (D2).
 */
class Reembolso(
    val terceiro: Pessoa,
    val valor: Dinheiro,
    val data: LocalDate,
    val id: String = novoId(),
) {
    init {
        if (!valor.ehPositivo()) throw ValorInvalido(valor)
    }

    override fun equals(other: Any?): Boolean = other is Reembolso && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Reembolso(${terceiro.nome}, ${valor.centavos}, $data)"
}
