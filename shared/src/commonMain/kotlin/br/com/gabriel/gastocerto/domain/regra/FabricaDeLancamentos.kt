package br.com.gabriel.gastocerto.domain.regra

import br.com.gabriel.gastocerto.domain.model.CarteiraMoedaEstrangeira
import br.com.gabriel.gastocerto.domain.model.CartaoDeCredito
import br.com.gabriel.gastocerto.domain.model.CompraParcelada
import br.com.gabriel.gastocerto.domain.model.ContaFixa
import br.com.gabriel.gastocerto.domain.model.ContaInvestimento
import br.com.gabriel.gastocerto.domain.model.ConversaoCambial
import br.com.gabriel.gastocerto.domain.model.Dinheiro
import br.com.gabriel.gastocerto.domain.model.Gasto
import br.com.gabriel.gastocerto.domain.model.Lancamento
import br.com.gabriel.gastocerto.domain.model.MeioDePagamento
import br.com.gabriel.gastocerto.domain.model.MesAno
import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.model.ReceitaRecorrente
import kotlinx.datetime.LocalDate

/**
 * Dados informados para registrar um gasto à vista (UC01).
 *
 * - Meio comum: [valor] em reais (obrigatório).
 * - Carteira em moeda estrangeira (RN05): [valorEmMoeda] em centavos da moeda e [cotacao] com 4 casas
 *   (D1); [valor] é ignorado e calculado pela conversão.
 * - [terceiro]: gasto em nome de quem vai reembolsar (RN03).
 * - Corretora: o gasto é um aporte realizado (RN11).
 */
data class DadosDoGasto(
    val meio: MeioDePagamento,
    val valor: Dinheiro?,
    val data: LocalDate,
    val descricao: String,
    val terceiro: Pessoa? = null,
    val valorEmMoeda: Long? = null,
    val cotacao: Long? = null,
)

/** Dados informados para registrar uma compra parcelada no cartão (UC02). */
data class DadosDaCompra(
    val cartao: CartaoDeCredito,
    val valorTotal: Dinheiro,
    val quantidade: Int,
    val dataCompra: LocalDate,
    val descricao: String,
    val terceiro: Pessoa? = null,
)

/**
 * **Factory** – reúne as variações de criação de lançamentos (simples, moeda estrangeira, terceiro,
 * aporte, parcelado e previstos do mês) e as validações comuns, para os casos de uso só pedirem
 * o lançamento pronto. Lança subclasses de [RegraVioladaException].
 */
class FabricaDeLancamentos {

    /**
     * UC01 – gasto à vista REALIZADO, com a competência dada pela política do meio (RN01).
     * RN09: valor > 0 (`ValorInvalido`). RN05: na carteira estrangeira, cotação > 0 (`CotacaoInvalida`)
     * e valor na moeda > 0; o valor em reais vem da conversão (D1).
     */
    fun gasto(dados: DadosDoGasto): Gasto {
        val conversao = if (dados.meio is CarteiraMoedaEstrangeira) conversaoDe(dados) else null
        val valor = conversao?.emReais() ?: dados.valor ?: Dinheiro(0)
        if (!valor.ehPositivo()) throw ValorInvalido(valor)
        return Gasto(
            valor = valor,
            data = dados.data,
            descricao = dados.descricao,
            competencia = dados.meio.politicaDeCompetencia().competenciaDe(dados.data),
            meio = dados.meio,
            terceiro = dados.terceiro,
            conversao = conversao,
        )
    }

    /**
     * UC02 – valida e cria a [CompraParcelada] (as parcelas saem de `gerarParcelas()`).
     * RN09: valor total > 0 e ao menos 1 centavo por parcela (`ValorInvalido`).
     * RN02: de 2 a 48 parcelas (`ParcelasForaDoLimite`).
     */
    fun compraParcelada(dados: DadosDaCompra): CompraParcelada {
        if (!dados.valorTotal.ehPositivo()) throw ValorInvalido(dados.valorTotal)
        if (dados.quantidade !in CompraParcelada.LIMITE_DE_PARCELAS) throw ParcelasForaDoLimite(dados.quantidade)
        return CompraParcelada(
            valorTotal = dados.valorTotal,
            quantidade = dados.quantidade,
            dataCompra = dados.dataCompra,
            descricao = dados.descricao,
            cartao = dados.cartao,
            terceiro = dados.terceiro,
        )
    }

    /**
     * UC03 – previstos do [mes] que está sendo aberto: contas fixas ativas (RN06), receitas
     * recorrentes ativas (RN07) e aportes planejados das corretoras ativas (RN11), cada um com
     * `origemId` (D3). Não remove duplicados: isso é feito no `FecharMesUseCase` (`semDuplicados`).
     */
    fun previstosDoMes(
        mes: MesAno,
        contasFixas: List<ContaFixa>,
        receitas: List<ReceitaRecorrente>,
        investimentos: List<ContaInvestimento>,
    ): List<Lancamento> =
        contasFixas.filter { it.ativa }.map { it.gerarPrevisto(mes) } +
            receitas.filter { it.ativa }.map { it.gerarPrevista(mes) } +
            investimentos.filter { it.ativo }.mapNotNull { it.gerarAportePrevisto(mes) }

    /** RN05 – a conversão exige cotação > 0 e valor na moeda > 0 (validados em [ConversaoCambial]). */
    private fun conversaoDe(dados: DadosDoGasto): ConversaoCambial {
        val cotacao = dados.cotacao ?: throw CotacaoInvalida(0)
        val valorEmMoeda = dados.valorEmMoeda ?: throw ValorInvalido(Dinheiro(0))
        return ConversaoCambial(valorOriginal = valorEmMoeda, cotacao = cotacao)
    }
}
