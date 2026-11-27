package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.model.Gasto
import br.com.gabriel.gastocerto.domain.model.Instituicao
import br.com.gabriel.gastocerto.domain.model.MeioDePagamento
import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.repositorio.CadastroRepository

class CadastroEmMemoriaRepository(private val banco: BancoEmMemoria) : CadastroRepository {

    override suspend fun pessoas(): List<Pessoa> = banco.pessoas.values.sortedBy { it.nome }

    override suspend fun instituicoes(): List<Instituicao> = banco.instituicoes.values.sortedBy { it.nome }

    override suspend fun meiosAtivos(): List<MeioDePagamento> =
        banco.meios.values.filter { it.ativo }.sortedBy { it.nome }.map { it.copia() }

    override suspend fun salvarPessoa(p: Pessoa) {
        banco.pessoas = banco.pessoas + (p.id to p)
    }

    override suspend fun salvarInstituicao(i: Instituicao) {
        banco.instituicoes = banco.instituicoes + (i.id to i)
    }

    override suspend fun salvarMeio(m: MeioDePagamento) {
        banco.meios = banco.meios + (m.id to m.copia())
    }

    override suspend fun possuiLancamentos(m: MeioDePagamento): Boolean =
        banco.lancamentos.values.any { it is Gasto && it.meio == m }
}
