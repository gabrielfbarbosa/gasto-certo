package br.com.gabriel.gastocerto.domain.repositorio

/**
 * Executa um bloco de gravações como **tudo ou nada**: se o bloco lançar exceção, nada do que
 * ele gravou permanece (usado no fechamento do mês, UC03). Suspensa porque o Room KMP só tem
 * transação suspensa (D4).
 */
interface Transacao {
    suspend fun <T> executar(bloco: suspend () -> T): T
}
