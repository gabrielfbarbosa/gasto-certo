package br.com.gabriel.gastocerto.domain.regra

import br.com.gabriel.gastocerto.domain.model.Dinheiro
import br.com.gabriel.gastocerto.domain.model.MesAno

/**
 * Violação de uma regra de negócio. É lançada pelo domínio ou pela [FabricaDeLancamentos]
 * e capturada pelo ViewModel, que mostra a [mensagem] ao usuário (sem crash).
 *
 * [regra] é o ID da RN violada (ex.: `"RN02"`), usado nos testes e nos registros.
 */
sealed class RegraVioladaException(
    val regra: String,
    val mensagem: String,
) : RuntimeException("$regra: $mensagem")

/** RN09 – o valor de um lançamento precisa ser maior que zero. */
class ValorInvalido(valor: Dinheiro) :
    RegraVioladaException("RN09", "o valor precisa ser maior que zero (informado: ${valor.centavos} centavos)")

/** RN05 – gasto em moeda estrangeira exige cotação maior que zero. */
class CotacaoInvalida(cotacao: Long) :
    RegraVioladaException("RN05", "a cotação precisa ser maior que zero (informada: $cotacao)")

/** RN02 – compra parcelada só pode ter de 2 a 48 parcelas. */
class ParcelasForaDoLimite(quantidade: Int) :
    RegraVioladaException("RN02", "a compra parcelada deve ter de 2 a 48 parcelas (informado: $quantidade)")

/** RN08 – competência fechada não aceita inclusão, edição nem exclusão de lançamentos. */
class CompetenciaFechada(mes: MesAno) :
    RegraVioladaException("RN08", "o mês ${mes.mes}/${mes.ano} está fechado; reabra-o para alterar lançamentos")

/** RN03 – o reembolso não pode ser maior que o saldo a receber do terceiro. */
class ReembolsoMaiorQueSaldo(valor: Dinheiro, saldo: Dinheiro) :
    RegraVioladaException(
        "RN03",
        "o reembolso (${valor.centavos} centavos) é maior que o saldo a receber (${saldo.centavos} centavos)",
    )

/** RN09 – meio de pagamento ou instituição com lançamentos não pode ser excluído, só desativado. */
class MeioComLancamentos(nome: String) :
    RegraVioladaException("RN09", "\"$nome\" tem lançamentos e não pode ser excluído; desative-o")
