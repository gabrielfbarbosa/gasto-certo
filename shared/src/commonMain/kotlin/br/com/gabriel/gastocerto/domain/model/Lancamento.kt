package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.LocalDate

/** Situação de um lançamento: PREVISTO (gerado por recorrência, valor a conferir) ou REALIZADO. */
enum class SituacaoDoLancamento { PREVISTO, REALIZADO }

/** Tipo de receita (RN07, RN11). */
enum class TipoDeReceita { SALARIO, AVULSA, VENDA_INVESTIMENTO }

/**
 * Registro de um gasto ou de uma receita.
 *
 * - [competencia]: mês ao qual o lançamento pertence nos resumos (RN01; calculado por quem cria).
 * - [origemId]: id da recorrência que gerou o PREVISTO; evita duplicar no fechamento do mês (D3).
 * - RN09: o [valor] é sempre maior que zero (`ValorInvalido`).
 * - Igualdade por [id] (D2).
 */
sealed class Lancamento(
    valor: Dinheiro,
    val data: LocalDate,
    val descricao: String,
    val competencia: MesAno,
    situacao: SituacaoDoLancamento,
    val origemId: String?,
    val id: String,
) {
    var valor: Dinheiro = valor
        private set

    var situacao: SituacaoDoLancamento = situacao
        private set

    init {
        if (!valor.ehPositivo()) throw ValorInvalido(valor)
    }

    /**
     * Confere um lançamento PREVISTO com o [valorReal] do mês e o torna REALIZADO (RN06, RN07, RN11).
     * RN09: o valor real também precisa ser maior que zero. Só previstos podem ser confirmados.
     */
    fun confirmar(valorReal: Dinheiro) {
        check(situacao == SituacaoDoLancamento.PREVISTO) { "só um lançamento PREVISTO pode ser confirmado" }
        if (!valorReal.ehPositivo()) throw ValorInvalido(valorReal)
        valor = valorReal
        situacao = SituacaoDoLancamento.REALIZADO
    }

    final override fun equals(other: Any?): Boolean = other is Lancamento && other.id == id
    final override fun hashCode(): Int = id.hashCode()
}

/**
 * Saída de dinheiro por um [meio] de pagamento.
 *
 * RN03 – pode estar em nome de um [terceiro] reembolsante: entra no total do meio e no gasto total,
 * e é descontado na sobra. RN05 – gasto em moeda estrangeira guarda a [conversao]; o [valor] é o
 * valor em reais.
 */
open class Gasto(
    valor: Dinheiro,
    data: LocalDate,
    descricao: String,
    competencia: MesAno,
    val meio: MeioDePagamento,
    val terceiro: Pessoa? = null,
    val conversao: ConversaoCambial? = null,
    situacao: SituacaoDoLancamento = SituacaoDoLancamento.REALIZADO,
    origemId: String? = null,
    id: String = novoId(),
) : Lancamento(valor, data, descricao, competencia, situacao, origemId, id) {

    /** RN03 – o gasto foi feito em nome de um terceiro que vai devolver o valor. */
    fun ehDeTerceiro(): Boolean = terceiro != null

    override fun toString(): String = "Gasto($descricao, ${valor.centavos}, $competencia, ${meio.nome})"
}

/**
 * RN02 – uma das parcelas de uma [CompraParcelada], rotulada `k/N` ([rotulo]).
 * Guarda o [compraId] da compra; a compra não tem tabela própria (D5).
 */
class Parcela(
    valor: Dinheiro,
    data: LocalDate,
    descricao: String,
    competencia: MesAno,
    meio: CartaoDeCredito,
    val compraId: String,
    val numero: Int,
    val totalDeParcelas: Int,
    terceiro: Pessoa? = null,
    id: String = novoId(),
) : Gasto(valor, data, descricao, competencia, meio, terceiro, id = id) {

    init {
        require(numero in 1..totalDeParcelas) { "parcela $numero fora de 1..$totalDeParcelas" }
    }

    /** RN02 – rótulo `k/N` (ex.: `2/6`). */
    fun rotulo(): String = "$numero/$totalDeParcelas"

    override fun toString(): String = "Parcela(${rotulo()}, $descricao, ${valor.centavos}, $competencia)"
}

/** Entrada de dinheiro: salário, receita avulsa ou venda de investimento (RN07, RN11). Não inclui reembolso (RN03). */
class Receita(
    valor: Dinheiro,
    data: LocalDate,
    descricao: String,
    competencia: MesAno,
    val tipo: TipoDeReceita,
    situacao: SituacaoDoLancamento = SituacaoDoLancamento.REALIZADO,
    origemId: String? = null,
    id: String = novoId(),
) : Lancamento(valor, data, descricao, competencia, situacao, origemId, id) {
    override fun toString(): String = "Receita($descricao, ${valor.centavos}, $competencia, $tipo)"
}
