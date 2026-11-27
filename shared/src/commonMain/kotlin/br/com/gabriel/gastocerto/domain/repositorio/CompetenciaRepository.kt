package br.com.gabriel.gastocerto.domain.repositorio

import br.com.gabriel.gastocerto.domain.model.Competencia
import br.com.gabriel.gastocerto.domain.model.MesAno

/** Porta de persistência das competências (RN08). Há uma por mês. */
interface CompetenciaRepository {
    /** A competência de [mes]; se ainda não existe, é criada ABERTA e gravada. */
    suspend fun buscarOuCriar(mes: MesAno): Competencia
    suspend fun salvar(c: Competencia)
}
