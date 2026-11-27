package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.CompetenciaPorDataDoGasto
import br.com.gabriel.gastocerto.domain.regra.CompetenciaPorFechamentoDeFatura
import br.com.gabriel.gastocerto.domain.regra.DiaInvalido
import br.com.gabriel.gastocerto.domain.regra.PoliticaDeCompetencia
import br.com.gabriel.gastocerto.domain.regra.TitularInvalido
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido

/**
 * Origem de um gasto (RF03). Cada tipo devolve a sua [PoliticaDeCompetencia] (Strategy, RN01),
 * para os casos de uso não precisarem de `when(meio)`.
 *
 * RN10 – todo meio tem um [titular] (usuário ou titular da família, nunca terceiro); uma
 * instituição pode ter vários meios de titulares diferentes.
 * RN09 – meio com lançamentos não é excluído, só desativado ([ativo] = `false`).
 * Igualdade por [id] (D2).
 */
sealed class MeioDePagamento(
    val nome: String,
    val titular: Pessoa,
    val instituicao: Instituicao,
    var ativo: Boolean,
    val id: String,
) {
    init {
        if (titular.papel == Papel.TERCEIRO) throw TitularInvalido(titular.nome)
    }

    abstract fun politicaDeCompetencia(): PoliticaDeCompetencia

    final override fun equals(other: Any?): Boolean = other is MeioDePagamento && other.id == id
    final override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "${this::class.simpleName}($nome, ${titular.nome})"
}

/** Cartão de crédito: a competência segue o fechamento e o vencimento da fatura (RN01); dias de 1 a 31 (RN09). */
class CartaoDeCredito(
    nome: String,
    titular: Pessoa,
    instituicao: Instituicao,
    val diaFechamento: Int,
    val diaVencimento: Int,
    ativo: Boolean = true,
    id: String = novoId(),
) : MeioDePagamento(nome, titular, instituicao, ativo, id) {

    init {
        if (diaFechamento !in 1..31) throw DiaInvalido("fechamento", diaFechamento)
        if (diaVencimento !in 1..31) throw DiaInvalido("vencimento", diaVencimento)
    }

    override fun politicaDeCompetencia(): PoliticaDeCompetencia =
        CompetenciaPorFechamentoDeFatura(diaFechamento, diaVencimento)
}

/** Conta corrente (débito e Pix): competência = mês da data do gasto (RN01). */
class ContaCorrente(
    nome: String,
    titular: Pessoa,
    instituicao: Instituicao,
    ativo: Boolean = true,
    id: String = novoId(),
) : MeioDePagamento(nome, titular, instituicao, ativo, id) {
    override fun politicaDeCompetencia(): PoliticaDeCompetencia = CompetenciaPorDataDoGasto()
}

/** Carteira em moeda estrangeira (ex.: Wise em dólar); os gastos exigem cotação (RN05). Competência = mês do gasto. */
class CarteiraMoedaEstrangeira(
    nome: String,
    titular: Pessoa,
    instituicao: Instituicao,
    val moeda: String,
    ativo: Boolean = true,
    id: String = novoId(),
) : MeioDePagamento(nome, titular, instituicao, ativo, id) {
    override fun politicaDeCompetencia(): PoliticaDeCompetencia = CompetenciaPorDataDoGasto()
}

/**
 * Conta em corretora (RN11). O [aportePlanejadoMensal], se informado, precisa ser maior que zero
 * e gera o aporte PREVISTO de cada mês ([gerarAportePrevisto]). Competência = mês do gasto.
 */
class ContaInvestimento(
    nome: String,
    titular: Pessoa,
    instituicao: Instituicao,
    val aportePlanejadoMensal: Dinheiro? = null,
    ativo: Boolean = true,
    id: String = novoId(),
) : MeioDePagamento(nome, titular, instituicao, ativo, id) {

    init {
        if (aportePlanejadoMensal != null && !aportePlanejadoMensal.ehPositivo()) {
            throw ValorInvalido(aportePlanejadoMensal)
        }
    }

    override fun politicaDeCompetencia(): PoliticaDeCompetencia = CompetenciaPorDataDoGasto()

    /**
     * RN11 – aporte PREVISTO do [mes] (um gasto nesta corretora), no dia 1, com `origemId` = [id] (D3).
     * Devolve `null` se não houver aporte planejado. O valor é ajustado ao confirmar.
     */
    fun gerarAportePrevisto(mes: MesAno): Gasto? = aportePlanejadoMensal?.let { valor ->
        Gasto(
            valor = valor,
            data = mes.dia(1),
            descricao = "Aporte planejado – $nome",
            competencia = mes,
            meio = this,
            situacao = SituacaoDoLancamento.PREVISTO,
            origemId = id,
        )
    }
}
