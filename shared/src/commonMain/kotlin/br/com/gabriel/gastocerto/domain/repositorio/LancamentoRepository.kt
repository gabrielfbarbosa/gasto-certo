package br.com.gabriel.gastocerto.domain.repositorio

import br.com.gabriel.gastocerto.domain.model.Gasto
import br.com.gabriel.gastocerto.domain.model.Lancamento
import br.com.gabriel.gastocerto.domain.model.MesAno
import br.com.gabriel.gastocerto.domain.model.Pessoa
import kotlinx.datetime.LocalDate

/**
 * Porta de persistência dos lançamentos (gastos, parcelas, receitas). Implementada por Room
 * (Android/iOS) e em memória (Web e testes). Gravar com o mesmo `id` substitui (D2).
 * O que é lido é uma cópia: alterar um objeto lido só vale depois de [salvar].
 */
interface LancamentoRepository {
    suspend fun salvar(l: Lancamento)
    suspend fun salvarTodos(l: List<Lancamento>)
    suspend fun excluir(l: Lancamento)

    /** Lançamentos cuja competência é [mes] (RN01). */
    suspend fun doMes(mes: MesAno): List<Lancamento>

    /** Lançamentos com data entre [inicio] e [fim], inclusive (semana e ano – RF15). */
    suspend fun doPeriodo(inicio: LocalDate, fim: LocalDate): List<Lancamento>

    /** Gastos feitos em nome do terceiro [p] (RN03). */
    suspend fun deTerceiro(p: Pessoa): List<Gasto>
}
