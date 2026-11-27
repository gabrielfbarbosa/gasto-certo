package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.model.Gasto
import br.com.gabriel.gastocerto.domain.model.Lancamento
import br.com.gabriel.gastocerto.domain.model.MesAno
import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.repositorio.LancamentoRepository
import kotlinx.datetime.LocalDate

class LancamentoEmMemoriaRepository(private val banco: BancoEmMemoria) : LancamentoRepository {

    override suspend fun salvar(l: Lancamento) {
        banco.lancamentos = banco.lancamentos + (l.id to l.copia())
    }

    override suspend fun salvarTodos(l: List<Lancamento>) {
        banco.lancamentos = banco.lancamentos + l.associate { it.id to it.copia() }
    }

    override suspend fun excluir(l: Lancamento) {
        banco.lancamentos = banco.lancamentos - l.id
    }

    override suspend fun doMes(mes: MesAno): List<Lancamento> =
        lidos { it.competencia == mes }

    override suspend fun doPeriodo(inicio: LocalDate, fim: LocalDate): List<Lancamento> =
        lidos { it.data in inicio..fim }

    override suspend fun deTerceiro(p: Pessoa): List<Gasto> =
        lidos { it is Gasto && it.terceiro == p }.filterIsInstance<Gasto>()

    private fun lidos(filtro: (Lancamento) -> Boolean): List<Lancamento> =
        banco.lancamentos.values.filter(filtro).sortedBy { it.data }.map { it.copia() }
}
