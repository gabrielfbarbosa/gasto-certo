package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.model.ContaFixa
import br.com.gabriel.gastocerto.domain.model.Instituicao
import br.com.gabriel.gastocerto.domain.model.Lancamento
import br.com.gabriel.gastocerto.domain.model.MeioDePagamento
import br.com.gabriel.gastocerto.domain.model.MesAno
import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.model.ReceitaRecorrente
import br.com.gabriel.gastocerto.domain.model.Reembolso
import br.com.gabriel.gastocerto.domain.model.SituacaoDaCompetencia

/**
 * Estado compartilhado pelos repositórios em memória (um "banco" por instância).
 *
 * Guarda **cópias** dos objetos (como um banco de verdade): o que entra é copiado ao gravar e o que
 * sai é copiado ao ler, então mexer num objeto lido não altera o banco sem `salvar`. Por isso uma
 * [foto] rasa das tabelas basta para a [TransacaoEmMemoria] desfazer as gravações.
 * Não é seguro para várias threads: Web e testes rodam numa só.
 */
class BancoEmMemoria {
    internal var pessoas = mapOf<String, Pessoa>()
    internal var instituicoes = mapOf<String, Instituicao>()
    internal var meios = mapOf<String, MeioDePagamento>()
    internal var lancamentos = mapOf<String, Lancamento>()
    internal var competencias = mapOf<MesAno, SituacaoDaCompetencia>()
    internal var contasFixas = mapOf<String, ContaFixa>()
    internal var receitas = mapOf<String, ReceitaRecorrente>()
    internal var reembolsos = mapOf<String, Reembolso>()

    internal class Foto(
        val pessoas: Map<String, Pessoa>,
        val instituicoes: Map<String, Instituicao>,
        val meios: Map<String, MeioDePagamento>,
        val lancamentos: Map<String, Lancamento>,
        val competencias: Map<MesAno, SituacaoDaCompetencia>,
        val contasFixas: Map<String, ContaFixa>,
        val receitas: Map<String, ReceitaRecorrente>,
        val reembolsos: Map<String, Reembolso>,
    )

    internal fun foto() = Foto(pessoas, instituicoes, meios, lancamentos, competencias, contasFixas, receitas, reembolsos)

    internal fun restaurar(foto: Foto) {
        pessoas = foto.pessoas
        instituicoes = foto.instituicoes
        meios = foto.meios
        lancamentos = foto.lancamentos
        competencias = foto.competencias
        contasFixas = foto.contasFixas
        receitas = foto.receitas
        reembolsos = foto.reembolsos
    }
}
