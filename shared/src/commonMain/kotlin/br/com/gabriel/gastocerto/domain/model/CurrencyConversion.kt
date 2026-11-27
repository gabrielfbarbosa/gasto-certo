package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.rule.InvalidAmount
import br.com.gabriel.gastocerto.domain.rule.InvalidExchangeRate

/**
 * RN05 – amount of an expense in a foreign currency (D1).
 *
 * [originalAmount] in cents of the currency (US$ 12.50 → `1250`); [exchangeRate] with 4 decimal places
 * (5.4321 → `54321`), greater than zero (`InvalidExchangeRate`). [toBrl] = amount × rate ÷ 10,000, half-up.
 */
data class CurrencyConversion(val originalAmount: Long, val exchangeRate: Long) {

    init {
        if (exchangeRate <= 0) throw InvalidExchangeRate(exchangeRate)
        if (originalAmount <= 0) throw InvalidAmount(Money(originalAmount))
    }

    fun toBrl(): Money = Money(originalAmount).convert(exchangeRate)
}
