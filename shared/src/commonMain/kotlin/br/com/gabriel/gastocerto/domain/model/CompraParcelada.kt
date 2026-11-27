package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.ParcelasForaDoLimite
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * RN02 – compra no cartão de crédito dividida em [quantidade] parcelas (2 a 48).
 *
 * **Creator + Information Expert**: [gerarParcelas] cria as parcelas. Não tem tabela própria:
 * é reconstruída a partir das parcelas, que guardam o [id] da compra em `compraId` (D5).
 * Todas as parcelas ficam em nome do [terceiro], se houver (RN03). Igualdade por [id] (D2).
 */
class CompraParcelada(
    val valorTotal: Dinheiro,
    val quantidade: Int,
    val dataCompra: LocalDate,
    val descricao: String,
    val cartao: CartaoDeCredito,
    val terceiro: Pessoa? = null,
    val id: String = novoId(),
) {
    init {
        if (!valorTotal.ehPositivo()) throw ValorInvalido(valorTotal)
        if (quantidade !in LIMITE_DE_PARCELAS) throw ParcelasForaDoLimite(quantidade)
    }

    /**
     * RN02 – gera as parcelas em competências consecutivas a partir da competência da 1ª,
     * dada pela política do cartão (RN01). Valor = total ÷ N em centavos, com o resto na 1ª;
     * a soma das parcelas é igual ao total. A data da parcela k é a data da compra + (k − 1) meses
     * (dia inexistente → último dia do mês), para os resumos por semana e ano.
     */
    fun gerarParcelas(): List<Parcela> {
        val politica = cartao.politicaDeCompetencia()
        var competencia = politica.competenciaDe(dataCompra)
        val valores = valorTotal.dividirEm(quantidade)
        return valores.mapIndexed { indice, valor ->
            if (indice > 0) competencia = competencia.proximo()
            Parcela(
                valor = valor,
                data = dataCompra.plus(indice, DateTimeUnit.MONTH),
                descricao = descricao,
                competencia = competencia,
                meio = cartao,
                compraId = id,
                numero = indice + 1,
                totalDeParcelas = quantidade,
                terceiro = terceiro,
            )
        }
    }

    override fun equals(other: Any?): Boolean = other is CompraParcelada && other.id == id
    override fun hashCode(): Int = id.hashCode()

    companion object {
        /** RN02 – de 2 a 48 parcelas. */
        val LIMITE_DE_PARCELAS = 2..48
    }
}
