package br.com.gabriel.gastocerto.domain.model

/**
 * Totais de um período (semana, mês ou ano) – RF14, RF15.
 *
 * - [gastoTotal] soma todos os gastos, inclusive os feitos em nome de terceiros (RN03).
 * - [gastosDeTerceiros]: parte do gasto total que será devolvida; é descontada na [sobra] (RN03, RN04).
 * - [porMeio] e [porTitular]: gasto total aberto por meio de pagamento e pelo titular do meio (RN10).
 * - Reembolso não entra: não é receita (RN03).
 */
data class ResumoDoPeriodo(
    val receitas: Dinheiro,
    val gastoTotal: Dinheiro,
    val gastosDeTerceiros: Dinheiro,
    val porMeio: Map<MeioDePagamento, Dinheiro>,
    val porTitular: Map<Pessoa, Dinheiro>,
) {
    /** RN04 – sobra = receitas − (gasto total − gastos de terceiros). Pode ser negativa. */
    fun sobra(): Dinheiro = receitas - (gastoTotal - gastosDeTerceiros)

    companion object {
        /**
         * Monta o resumo a partir dos [lancamentos] do período (PREVISTOS e REALIZADOS: os previstos
         * entram pelo valor previsto até serem conferidos).
         *
         * RN10 – com [titular], só entram os gastos dos meios desse titular; as receitas não têm
         * titular e continuam todas (são da família).
         */
        fun de(lancamentos: List<Lancamento>, titular: Pessoa? = null): ResumoDoPeriodo {
            val gastos = lancamentos.filterIsInstance<Gasto>()
                .filter { titular == null || it.meio.titular == titular }
            val receitas = lancamentos.filterIsInstance<Receita>()
            return ResumoDoPeriodo(
                receitas = receitas.map { it.valor }.somar(),
                gastoTotal = gastos.map { it.valor }.somar(),
                gastosDeTerceiros = gastos.filter { it.ehDeTerceiro() }.map { it.valor }.somar(),
                porMeio = gastos.groupBy { it.meio }.mapValues { (_, doMeio) -> doMeio.map { it.valor }.somar() },
                porTitular = gastos.groupBy { it.meio.titular }.mapValues { (_, doTitular) -> doTitular.map { it.valor }.somar() },
            )
        }
    }
}
