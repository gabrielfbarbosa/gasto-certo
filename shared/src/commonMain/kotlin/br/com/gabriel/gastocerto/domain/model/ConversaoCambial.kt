package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.CotacaoInvalida
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido

/**
 * RN05 – valor de um gasto em moeda estrangeira (D1).
 *
 * [valorOriginal] em centavos da moeda (US$ 12,50 → `1250`); [cotacao] com 4 casas
 * (5,4321 → `54321`), maior que zero (`CotacaoInvalida`). [emReais] = valor × cotação ÷ 10.000, half-up.
 */
data class ConversaoCambial(val valorOriginal: Long, val cotacao: Long) {

    init {
        if (cotacao <= 0) throw CotacaoInvalida(cotacao)
        if (valorOriginal <= 0) throw ValorInvalido(Dinheiro(valorOriginal))
    }

    fun emReais(): Dinheiro = Dinheiro(valorOriginal).converter(cotacao)
}
