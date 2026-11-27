package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.model.Competencia
import br.com.gabriel.gastocerto.domain.model.MesAno
import br.com.gabriel.gastocerto.domain.model.SituacaoDaCompetencia
import br.com.gabriel.gastocerto.domain.repositorio.CompetenciaRepository

class CompetenciaEmMemoriaRepository(private val banco: BancoEmMemoria) : CompetenciaRepository {

    override suspend fun buscarOuCriar(mes: MesAno): Competencia {
        val situacao = banco.competencias[mes] ?: SituacaoDaCompetencia.ABERTA.also {
            banco.competencias = banco.competencias + (mes to it)
        }
        return Competencia(mes, situacao)
    }

    override suspend fun salvar(c: Competencia) {
        banco.competencias = banco.competencias + (c.mesAno to c.situacao)
    }
}
