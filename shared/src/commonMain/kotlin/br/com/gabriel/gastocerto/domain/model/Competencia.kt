package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.CompetenciaFechada

/** Situação de uma competência (mês): ABERTA aceita lançamentos; FECHADA não (RN08). */
enum class SituacaoDaCompetencia { ABERTA, FECHADA }

/**
 * RN08 – um mês do controle. Competência FECHADA não aceita inclusão, edição nem exclusão
 * de lançamentos; reabrir é explícito ([reabrir]). Igualdade pelo [mesAno] (há uma por mês).
 */
class Competencia(
    val mesAno: MesAno,
    situacao: SituacaoDaCompetencia = SituacaoDaCompetencia.ABERTA,
) {
    var situacao: SituacaoDaCompetencia = situacao
        private set

    /** RN08 – fecha o mês. Fechar um mês já fechado lança [CompetenciaFechada]. */
    fun fechar() {
        garantirAberta()
        situacao = SituacaoDaCompetencia.FECHADA
    }

    /** RN08 – reabre o mês para correções. Reabrir um mês aberto não muda nada. */
    fun reabrir() {
        situacao = SituacaoDaCompetencia.ABERTA
    }

    /** RN08 – lança [CompetenciaFechada] se o mês estiver fechado; chamado antes de gravar lançamentos. */
    fun garantirAberta() {
        if (situacao == SituacaoDaCompetencia.FECHADA) throw CompetenciaFechada(mesAno)
    }

    override fun equals(other: Any?): Boolean = other is Competencia && other.mesAno == mesAno
    override fun hashCode(): Int = mesAno.hashCode()
    override fun toString(): String = "Competencia(${mesAno.mes}/${mesAno.ano}, $situacao)"
}
