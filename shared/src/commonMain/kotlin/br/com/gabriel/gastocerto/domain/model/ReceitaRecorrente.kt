package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.DiaInvalido
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido

/**
 * RN07 – entrada mensal em dia fixo (salário).
 *
 * **Creator**: [gerarPrevista] cria a receita PREVISTA de cada mês. [valor] > 0 (RN09) e
 * [diaRecebimento] entre 1 e 31 (RN09). Só as ativas geram previstas. Igualdade por [id] (D2).
 */
class ReceitaRecorrente(
    val descricao: String,
    val valor: Dinheiro,
    val diaRecebimento: Int,
    var ativa: Boolean = true,
    val id: String = novoId(),
) {
    init {
        if (!valor.ehPositivo()) throw ValorInvalido(valor)
        if (diaRecebimento !in 1..31) throw DiaInvalido("recebimento", diaRecebimento)
    }

    /**
     * RN07 – receita PREVISTA (tipo SALARIO) do [mes], no dia de recebimento (dia inexistente →
     * último dia do mês), com `origemId` = [id] (D3).
     */
    fun gerarPrevista(mes: MesAno): Receita = Receita(
        valor = valor,
        data = mes.dia(diaRecebimento),
        descricao = descricao,
        competencia = mes,
        tipo = TipoDeReceita.SALARIO,
        situacao = SituacaoDoLancamento.PREVISTO,
        origemId = id,
    )

    override fun equals(other: Any?): Boolean = other is ReceitaRecorrente && other.id == id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "ReceitaRecorrente($descricao, dia $diaRecebimento)"
}
