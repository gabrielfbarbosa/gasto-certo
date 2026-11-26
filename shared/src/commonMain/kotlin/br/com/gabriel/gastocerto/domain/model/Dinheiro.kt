package br.com.gabriel.gastocerto.domain.model

import kotlin.jvm.JvmInline

/**
 * Valor monetário em **centavos** (`Long`), sem ponto flutuante — RNF04.
 *
 * Os cálculos de dinheiro ficam aqui (Information Expert): soma, subtração,
 * divisão em parcelas sem perder centavos (RN02) e conversão de moeda estrangeira (RN05).
 * Pode ser negativo (ex.: sobra de um mês com mais gastos que receitas).
 */
@JvmInline
value class Dinheiro(val centavos: Long) {

    operator fun plus(outro: Dinheiro): Dinheiro = Dinheiro(centavos + outro.centavos)

    operator fun minus(outro: Dinheiro): Dinheiro = Dinheiro(centavos - outro.centavos)

    /**
     * RN02 – divide o valor em [n] partes iguais em centavos; **o resto vai para a 1ª parte**.
     * A soma das partes é sempre igual ao valor original (ex.: R$ 100,00 em 3 = 33,34 + 33,33 + 33,33).
     *
     * O limite de 2 a 48 parcelas é regra de negócio e é validado na criação da compra
     * (`FabricaDeLancamentos`); aqui só se exige [n] ≥ 1.
     */
    fun dividirEm(n: Int): List<Dinheiro> {
        require(n >= 1) { "n deve ser pelo menos 1, mas foi $n" }
        val base = centavos / n
        val resto = centavos % n
        return List(n) { i -> Dinheiro(if (i == 0) base + resto else base) }
    }

    /**
     * RN05 – converte um valor em moeda estrangeira para reais.
     *
     * Este [Dinheiro] está em centavos da moeda estrangeira; [cotacao] tem 4 casas decimais
     * em `Long` (5,4321 → `54321`), decisão D1. Resultado = centavos × cotação ÷ 10.000,
     * arredondado ao centavo pelo critério *half-up* (0,5 centavo arredonda para longe do zero).
     *
     * A cotação precisa ser maior que zero; a mensagem de regra (`CotacaoInvalida`) é
     * lançada antes, na criação do gasto.
     */
    fun converter(cotacao: Long): Dinheiro {
        require(cotacao > 0) { "cotação deve ser maior que zero, mas foi $cotacao" }
        val produto = centavos * cotacao
        val quociente = produto / ESCALA_DA_COTACAO
        val resto = produto % ESCALA_DA_COTACAO
        val arredondamento = when {
            resto * 2 >= ESCALA_DA_COTACAO -> 1
            resto * 2 <= -ESCALA_DA_COTACAO -> -1
            else -> 0
        }
        return Dinheiro(quociente + arredondamento)
    }

    /** RN09 – um valor de lançamento só é válido se for maior que zero. */
    fun ehPositivo(): Boolean = centavos > 0

    companion object {
        /** Cotação com 4 casas decimais: 1,0000 = 10.000 (D1). */
        const val ESCALA_DA_COTACAO: Long = 10_000
    }
}
