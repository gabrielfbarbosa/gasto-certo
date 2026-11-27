package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.DiaInvalido
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido

/**
 * RN06 – despesa mensal recorrente (água, luz, internet, mercado) paga por um [meio].
 *
 * **Creator**: [gerarPrevisto] cria o gasto PREVISTO de cada mês. [valorPrevisto] > 0 (RN09)
 * e [diaVencimento] entre 1 e 31 (RN09). Só as ativas geram previstos. Igualdade por [id] (D2).
 */
class ContaFixa(
    val descricao: String,
    val valorPrevisto: Dinheiro,
    val diaVencimento: Int,
    val meio: MeioDePagamento,
    var ativa: Boolean = true,
    val id: String = novoId(),
) {
    init {
        if (!valorPrevisto.ehPositivo()) throw ValorInvalido(valorPrevisto)
        if (diaVencimento !in 1..31) throw DiaInvalido("vencimento", diaVencimento)
    }

    /**
     * RN06 – gasto PREVISTO do [mes], no dia de vencimento (dia inexistente → último dia do mês),
     * com `origemId` = [id] para não ser gerado duas vezes (D3). O valor é ajustado ao confirmar.
     */
    fun gerarPrevisto(mes: MesAno): Gasto = Gasto(
        valor = valorPrevisto,
        data = mes.dia(diaVencimento),
        descricao = descricao,
        competencia = mes,
        meio = meio,
        situacao = SituacaoDoLancamento.PREVISTO,
        origemId = id,
    )

    override fun equals(other: Any?): Boolean = other is ContaFixa && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "ContaFixa($descricao, dia $diaVencimento)"
}
