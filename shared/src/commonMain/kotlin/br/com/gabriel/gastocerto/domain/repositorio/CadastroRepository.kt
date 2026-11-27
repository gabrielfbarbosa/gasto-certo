package br.com.gabriel.gastocerto.domain.repositorio

import br.com.gabriel.gastocerto.domain.model.Instituicao
import br.com.gabriel.gastocerto.domain.model.MeioDePagamento
import br.com.gabriel.gastocerto.domain.model.Pessoa

/** Porta de persistência dos cadastros: pessoas, instituições e meios de pagamento (UC06). */
interface CadastroRepository {
    suspend fun pessoas(): List<Pessoa>
    suspend fun instituicoes(): List<Instituicao>
    suspend fun meiosAtivos(): List<MeioDePagamento>
    suspend fun salvarPessoa(p: Pessoa)
    suspend fun salvarInstituicao(i: Instituicao)
    suspend fun salvarMeio(m: MeioDePagamento)

    /** RN09 – meio com lançamentos não pode ser excluído, só desativado. */
    suspend fun possuiLancamentos(m: MeioDePagamento): Boolean
}
