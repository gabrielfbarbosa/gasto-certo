package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.ReembolsoMaiorQueSaldo

/** RN03 – quanto um [terceiro] ainda deve devolver: gastos em nome dele − reembolsos recebidos. */
data class SaldoAReceber(val terceiro: Pessoa, val valor: Dinheiro) {

    /** RN03 – lança [ReembolsoMaiorQueSaldo] se o reembolso de [valor] passar do saldo. */
    fun validarReembolso(valor: Dinheiro) {
        if (valor.centavos > this.valor.centavos) throw ReembolsoMaiorQueSaldo(valor, this.valor)
    }

    companion object {
        /** Calcula o saldo do [terceiro]; gastos e reembolsos de outras pessoas são ignorados. */
        fun de(terceiro: Pessoa, gastos: List<Gasto>, reembolsos: List<Reembolso>): SaldoAReceber {
            val devido = gastos.filter { it.terceiro == terceiro }.map { it.valor }.somar()
            val devolvido = reembolsos.filter { it.terceiro == terceiro }.map { it.valor }.somar()
            return SaldoAReceber(terceiro, devido - devolvido)
        }
    }
}
