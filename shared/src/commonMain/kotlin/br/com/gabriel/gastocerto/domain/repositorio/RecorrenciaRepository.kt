package br.com.gabriel.gastocerto.domain.repositorio

import br.com.gabriel.gastocerto.domain.model.ContaFixa
import br.com.gabriel.gastocerto.domain.model.ContaInvestimento
import br.com.gabriel.gastocerto.domain.model.ReceitaRecorrente

/** Porta de persistência das recorrências que geram previstos (RN06, RN07, RN11). */
interface RecorrenciaRepository {
    suspend fun contasFixasAtivas(): List<ContaFixa>
    suspend fun receitasAtivas(): List<ReceitaRecorrente>

    /** Corretoras ativas com aporte planejado mensal (RN11). */
    suspend fun investimentosComAporte(): List<ContaInvestimento>
    suspend fun salvarContaFixa(c: ContaFixa)
    suspend fun salvarReceita(r: ReceitaRecorrente)
}
