package br.com.gabriel.gastocerto.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * Mês de um ano (ex.: novembro de 2026): a **competência** à qual um lançamento pertence.
 *
 * Usado pela RN01 (mês da fatura), pela RN06/RN07 (dia de vencimento/recebimento dos previstos)
 * e pelo fechamento do mês (UC03).
 */
data class MesAno(val ano: Int, val mes: Int) {

    init {
        require(mes in 1..12) { "mês deve estar entre 1 e 12, mas foi $mes" }
    }

    /** O mês seguinte (dezembro vira janeiro do ano seguinte). */
    fun proximo(): MesAno = if (mes == 12) MesAno(ano + 1, 1) else MesAno(ano, mes + 1)

    /** O mês anterior (janeiro vira dezembro do ano anterior). Usado na data de fechamento da fatura. */
    fun anterior(): MesAno = if (mes == 1) MesAno(ano - 1, 12) else MesAno(ano, mes - 1)

    /**
     * A data deste mês no [dia] informado. **Dia inexistente no mês vira o último dia do mês**
     * (ex.: dia 31 em fevereiro de 2027 → 28/02/2027), regra usada pelas contas fixas (RN06),
     * pelo salário (RN07) e pelo fechamento do cartão (RN01).
     */
    fun dia(dia: Int): LocalDate {
        require(dia in 1..31) { "dia deve estar entre 1 e 31, mas foi $dia" }
        return LocalDate(ano, mes, minOf(dia, quantidadeDeDias()))
    }

    private fun quantidadeDeDias(): Int = YearMonth(ano, mes).numberOfDays
}
