package br.com.gabriel.gastocerto.domain.model

import kotlin.jvm.JvmInline

/**
 * Monetary amount in **cents** (`Long`), with no floating point — RNF04.
 *
 * Money arithmetic lives here (Information Expert): addition, subtraction, splitting into
 * installments without losing cents (RN02) and foreign currency conversion (RN05).
 * May be negative (e.g. the surplus of a month with more expenses than income).
 */
@JvmInline
value class Money(val cents: Long) {

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    /**
     * RN02 – splits the amount into [n] equal parts in cents; **the remainder goes to the 1st part**.
     * The parts always add up to the original amount (e.g. R$ 100.00 in 3 = 33.34 + 33.33 + 33.33).
     *
     * The 2-to-48 installment limit is a business rule checked when the purchase is created
     * (`EntryFactory`); here only [n] ≥ 1 is required.
     */
    fun splitInto(n: Int): List<Money> {
        require(n >= 1) { "n must be at least 1, but was $n" }
        val base = cents / n
        val remainder = cents % n
        return List(n) { i -> Money(if (i == 0) base + remainder else base) }
    }

    /**
     * RN05 – converts an amount in a foreign currency to reais.
     *
     * This [Money] holds cents of the foreign currency; [exchangeRate] has 4 decimal places
     * as a `Long` (5.4321 → `54321`), decision D1. Result = cents × rate ÷ 10,000,
     * rounded to the cent *half-up* (half a cent rounds away from zero).
     *
     * The rate must be greater than zero; the rule exception (`InvalidExchangeRate`) is thrown
     * earlier, when the expense is created.
     */
    fun convert(exchangeRate: Long): Money {
        require(exchangeRate > 0) { "exchange rate must be greater than zero, but was $exchangeRate" }
        val product = cents * exchangeRate
        val quotient = product / EXCHANGE_RATE_SCALE
        val remainder = product % EXCHANGE_RATE_SCALE
        val rounding = when {
            remainder * 2 >= EXCHANGE_RATE_SCALE -> 1
            remainder * 2 <= -EXCHANGE_RATE_SCALE -> -1
            else -> 0
        }
        return Money(quotient + rounding)
    }

    /** RN09 – an entry amount is only valid when greater than zero. */
    fun isPositive(): Boolean = cents > 0

    companion object {
        /** Exchange rate with 4 decimal places: 1.0000 = 10,000 (D1). */
        const val EXCHANGE_RATE_SCALE: Long = 10_000
    }
}

/** Adds up a list of amounts (empty list = zero). Internal to the domain. */
internal fun Iterable<Money>.sum(): Money = fold(Money(0)) { total, amount -> total + amount }
