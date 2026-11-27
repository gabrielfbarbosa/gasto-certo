package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.model.Reembolso
import br.com.gabriel.gastocerto.domain.repositorio.ReembolsoRepository

class ReembolsoEmMemoriaRepository(private val banco: BancoEmMemoria) : ReembolsoRepository {

    override suspend fun salvar(r: Reembolso) {
        banco.reembolsos = banco.reembolsos + (r.id to r)
    }

    override suspend fun deTerceiro(p: Pessoa): List<Reembolso> =
        banco.reembolsos.values.filter { it.terceiro == p }.sortedBy { it.data }
}
