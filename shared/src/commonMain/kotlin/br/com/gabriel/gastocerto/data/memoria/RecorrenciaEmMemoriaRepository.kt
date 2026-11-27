package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.model.ContaFixa
import br.com.gabriel.gastocerto.domain.model.ContaInvestimento
import br.com.gabriel.gastocerto.domain.model.ReceitaRecorrente
import br.com.gabriel.gastocerto.domain.repositorio.RecorrenciaRepository

class RecorrenciaEmMemoriaRepository(private val banco: BancoEmMemoria) : RecorrenciaRepository {

    override suspend fun contasFixasAtivas(): List<ContaFixa> =
        banco.contasFixas.values.filter { it.ativa }.sortedBy { it.diaVencimento }.map { it.copia() }

    override suspend fun receitasAtivas(): List<ReceitaRecorrente> =
        banco.receitas.values.filter { it.ativa }.sortedBy { it.diaRecebimento }.map { it.copia() }

    override suspend fun investimentosComAporte(): List<ContaInvestimento> =
        banco.meios.values.filterIsInstance<ContaInvestimento>()
            .filter { it.ativo && it.aportePlanejadoMensal != null }
            .sortedBy { it.nome }
            .map { it.copia() as ContaInvestimento }

    override suspend fun salvarContaFixa(c: ContaFixa) {
        banco.contasFixas = banco.contasFixas + (c.id to c.copia())
    }

    override suspend fun salvarReceita(r: ReceitaRecorrente) {
        banco.receitas = banco.receitas + (r.id to r.copia())
    }
}
