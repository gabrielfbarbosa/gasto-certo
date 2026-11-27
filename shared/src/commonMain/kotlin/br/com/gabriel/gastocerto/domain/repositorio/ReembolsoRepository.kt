package br.com.gabriel.gastocerto.domain.repositorio

import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.model.Reembolso

/** Porta de persistência dos reembolsos recebidos de terceiros (RN03). */
interface ReembolsoRepository {
    suspend fun salvar(r: Reembolso)
    suspend fun deTerceiro(p: Pessoa): List<Reembolso>
}
