package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate

/**
 * RF13 – fatura de um [cartao] na [competencia] (o mês do vencimento, RN01). **Não é gravada**:
 * é montada a partir do cartão e dos gastos ([de]).
 */
data class Fatura(
    val cartao: CartaoDeCredito,
    val competencia: MesAno,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
    val gastos: List<Gasto>,
) {
    /** Soma dos gastos e parcelas da fatura. */
    fun total(): Dinheiro = gastos.map { it.valor }.somar()

    companion object {
        /**
         * Monta a fatura do [cartao] que vence em [mes]. Vencimento = dia de vencimento em [mes];
         * fechamento no mesmo mês se `diaVencimento > diaFechamento`, senão no mês anterior (RN01).
         * Dia inexistente → último dia do mês. Só entram os gastos desse cartão nessa competência.
         */
        fun de(cartao: CartaoDeCredito, mes: MesAno, gastos: List<Gasto>): Fatura {
            val mesDoFechamento = if (cartao.diaVencimento > cartao.diaFechamento) mes else mes.anterior()
            return Fatura(
                cartao = cartao,
                competencia = mes,
                dataFechamento = mesDoFechamento.dia(cartao.diaFechamento),
                dataVencimento = mes.dia(cartao.diaVencimento),
                gastos = gastos.filter { it.meio == cartao && it.competencia == mes },
            )
        }
    }
}
