package br.com.gabriel.gastocerto.domain.regra

import br.com.gabriel.gastocerto.domain.model.MesAno
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * RN01 – **Strategy**: decide a competência (mês) de um gasto a partir da data.
 *
 * Cada meio de pagamento devolve a sua política em `politicaDeCompetencia()`; os casos de uso
 * só chamam [competenciaDe], sem `when(meio)`. Um novo tipo de meio é uma nova política.
 */
interface PoliticaDeCompetencia {
    fun competenciaDe(data: LocalDate): MesAno
}

/**
 * RN01 – cartão de crédito. A compra feita **até** o dia de fechamento entra na fatura do ciclo
 * atual; **depois** dele, na seguinte. A competência é o **mês do vencimento** da fatura:
 * se [diaVencimento] > [diaFechamento], vence no mesmo mês do fechamento; senão, no mês seguinte.
 *
 * Dia de fechamento inexistente no mês (ex.: 31 em novembro) vira o último dia do mês ([MesAno.dia]).
 */
class CompetenciaPorFechamentoDeFatura(
    private val diaFechamento: Int,
    private val diaVencimento: Int,
) : PoliticaDeCompetencia {

    init {
        require(diaFechamento in 1..31) { "dia de fechamento deve estar entre 1 e 31, mas foi $diaFechamento" }
        require(diaVencimento in 1..31) { "dia de vencimento deve estar entre 1 e 31, mas foi $diaVencimento" }
    }

    override fun competenciaDe(data: LocalDate): MesAno {
        val mesDaCompra = MesAno(data.year, data.month.number)
        val mesDoFechamento =
            if (data <= mesDaCompra.dia(diaFechamento)) mesDaCompra else mesDaCompra.proximo()
        return if (diaVencimento > diaFechamento) mesDoFechamento else mesDoFechamento.proximo()
    }
}

/** RN01 – conta corrente, débito, Pix, carteira estrangeira e corretora: o mês da data do gasto. */
class CompetenciaPorDataDoGasto : PoliticaDeCompetencia {
    override fun competenciaDe(data: LocalDate): MesAno = MesAno(data.year, data.month.number)
}
