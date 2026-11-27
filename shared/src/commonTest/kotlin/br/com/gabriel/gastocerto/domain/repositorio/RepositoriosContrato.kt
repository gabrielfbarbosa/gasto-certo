package br.com.gabriel.gastocerto.domain.repositorio

import br.com.gabriel.gastocerto.domain.model.CartaoDeCredito
import br.com.gabriel.gastocerto.domain.model.CompraParcelada
import br.com.gabriel.gastocerto.domain.model.Competencia
import br.com.gabriel.gastocerto.domain.model.ContaCorrente
import br.com.gabriel.gastocerto.domain.model.ContaFixa
import br.com.gabriel.gastocerto.domain.model.ContaInvestimento
import br.com.gabriel.gastocerto.domain.model.ConversaoCambial
import br.com.gabriel.gastocerto.domain.model.Dinheiro
import br.com.gabriel.gastocerto.domain.model.Gasto
import br.com.gabriel.gastocerto.domain.model.Instituicao
import br.com.gabriel.gastocerto.domain.model.Lancamento
import br.com.gabriel.gastocerto.domain.model.MesAno
import br.com.gabriel.gastocerto.domain.model.Papel
import br.com.gabriel.gastocerto.domain.model.Parcela
import br.com.gabriel.gastocerto.domain.model.Pessoa
import br.com.gabriel.gastocerto.domain.model.Receita
import br.com.gabriel.gastocerto.domain.model.ReceitaRecorrente
import br.com.gabriel.gastocerto.domain.model.Reembolso
import br.com.gabriel.gastocerto.domain.model.SituacaoDaCompetencia
import br.com.gabriel.gastocerto.domain.model.SituacaoDoLancamento
import br.com.gabriel.gastocerto.domain.model.TipoDeReceita
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** As 5 portas de repositório + a transação, ligadas ao mesmo banco. */
class Repositorios(
    val lancamentos: LancamentoRepository,
    val competencias: CompetenciaRepository,
    val cadastros: CadastroRepository,
    val recorrencias: RecorrenciaRepository,
    val reembolsos: ReembolsoRepository,
    val transacao: Transacao,
)

/**
 * Contrato que toda implementação dos repositórios cumpre (memória agora, Room na 8.16):
 * gravar com o mesmo id substitui, consultas filtram certo, o que é lido é uma cópia
 * e a [Transacao] desfaz tudo quando o bloco falha.
 */
abstract class RepositoriosContrato {

    /** Um banco vazio a cada teste. */
    abstract fun novosRepositorios(): Repositorios

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val pai = Pessoa("Pai", Papel.TERCEIRO)
    private val nubankInst = Instituicao("Nubank")
    private val nubank = CartaoDeCredito("Nubank", gabriel, nubankInst, diaFechamento = 5, diaVencimento = 12)
    private val inter = ContaCorrente("Inter", gabriel, Instituicao("Inter"))
    private val novembro = MesAno(2026, 11)
    private val dezembro = MesAno(2026, 12)

    private fun gasto(valor: Long, data: LocalDate, competencia: MesAno = novembro, terceiro: Pessoa? = null) =
        Gasto(Dinheiro(valor), data, "gasto", competencia, inter, terceiro)

    // ---------- LancamentoRepository ----------

    @Test
    fun `lancamentos do mes filtram pela competencia`() = runTest {
        val r = novosRepositorios()
        val emNovembro = gasto(1_000, LocalDate(2026, 11, 30))
        val emDezembro = gasto(2_000, LocalDate(2026, 11, 30), competencia = dezembro)
        r.lancamentos.salvarTodos(listOf(emNovembro, emDezembro))

        assertEquals(listOf<Lancamento>(emNovembro), r.lancamentos.doMes(novembro))
        assertEquals(listOf<Lancamento>(emDezembro), r.lancamentos.doMes(dezembro))
    }

    @Test
    fun `lancamentos do periodo filtram pela data com as pontas incluidas`() = runTest {
        val r = novosRepositorios()
        val segunda = gasto(1, LocalDate(2026, 11, 23))
        val domingo = gasto(2, LocalDate(2026, 11, 29))
        val depois = gasto(3, LocalDate(2026, 11, 30))
        r.lancamentos.salvarTodos(listOf(depois, domingo, segunda))

        assertEquals(
            listOf<Lancamento>(segunda, domingo),
            r.lancamentos.doPeriodo(LocalDate(2026, 11, 23), LocalDate(2026, 11, 29)),
        )
    }

    @Test
    fun `salvar com o mesmo id substitui e excluir remove`() = runTest {
        val r = novosRepositorios()
        val previsto = Gasto(Dinheiro(20_000), LocalDate(2026, 11, 15), "Luz", novembro, inter, situacao = SituacaoDoLancamento.PREVISTO)
        r.lancamentos.salvar(previsto)

        previsto.confirmar(Dinheiro(18_000))
        r.lancamentos.salvar(previsto)
        val lido = r.lancamentos.doMes(novembro).single()
        assertEquals(Dinheiro(18_000), lido.valor)
        assertEquals(SituacaoDoLancamento.REALIZADO, lido.situacao)

        r.lancamentos.excluir(lido)
        assertTrue(r.lancamentos.doMes(novembro).isEmpty())
    }

    @Test
    fun `alterar um lancamento lido nao muda o banco sem salvar`() = runTest {
        val r = novosRepositorios()
        r.lancamentos.salvar(Gasto(Dinheiro(20_000), LocalDate(2026, 11, 15), "Luz", novembro, inter, situacao = SituacaoDoLancamento.PREVISTO))

        r.lancamentos.doMes(novembro).single().confirmar(Dinheiro(1))

        assertEquals(Dinheiro(20_000), r.lancamentos.doMes(novembro).single().valor)
    }

    @Test
    fun `parcelas receitas e conversao voltam com todos os campos`() = runTest {
        val r = novosRepositorios()
        val parcelas = CompraParcelada(Dinheiro(10_000), 2, LocalDate(2026, 11, 3), "TV", nubank, pai).gerarParcelas()
        val salario = Receita(Dinheiro(800_000), LocalDate(2026, 11, 5), "Salário", novembro, TipoDeReceita.SALARIO,
            SituacaoDoLancamento.PREVISTO, origemId = "rec-1")
        val dolar = Gasto(Dinheiro(6_790), LocalDate(2026, 11, 10), "App", novembro, inter, conversao = ConversaoCambial(1_250, 54_321))
        r.lancamentos.salvarTodos(parcelas + salario + dolar)

        val lidos = r.lancamentos.doMes(novembro)
        val parcela = assertIs<Parcela>(lidos.first { it.id == parcelas[0].id })
        assertEquals("1/2", parcela.rotulo())
        assertEquals(parcelas[0].compraId, parcela.compraId)
        assertEquals(pai, parcela.terceiro)
        val receita = assertIs<Receita>(lidos.first { it.id == salario.id })
        assertEquals(TipoDeReceita.SALARIO, receita.tipo)
        assertEquals("rec-1", receita.origemId)
        assertEquals(SituacaoDoLancamento.PREVISTO, receita.situacao)
        assertEquals(ConversaoCambial(1_250, 54_321), assertIs<Gasto>(lidos.first { it.id == dolar.id }).conversao)
    }

    @Test
    fun `gastos de terceiro trazem so os gastos em nome dele`() = runTest {
        val r = novosRepositorios()
        val doPai = gasto(5_000, LocalDate(2026, 11, 10), terceiro = pai)
        r.lancamentos.salvarTodos(listOf(doPai, gasto(7_000, LocalDate(2026, 11, 11))))

        assertEquals(listOf<Gasto>(doPai), r.lancamentos.deTerceiro(pai))
    }

    // ---------- CompetenciaRepository ----------

    @Test
    fun `buscarOuCriar cria a competencia aberta e salvar grava a situacao`() = runTest {
        val r = novosRepositorios()
        val competencia = r.competencias.buscarOuCriar(novembro)
        assertEquals(SituacaoDaCompetencia.ABERTA, competencia.situacao)

        competencia.fechar()
        assertEquals(SituacaoDaCompetencia.ABERTA, r.competencias.buscarOuCriar(novembro).situacao) // sem salvar
        r.competencias.salvar(competencia)
        assertEquals(SituacaoDaCompetencia.FECHADA, r.competencias.buscarOuCriar(novembro).situacao)
    }

    // ---------- CadastroRepository ----------

    @Test
    fun `cadastros sao gravados e so os meios ativos sao listados`() = runTest {
        val r = novosRepositorios()
        r.cadastros.salvarPessoa(gabriel)
        r.cadastros.salvarInstituicao(nubankInst)
        r.cadastros.salvarMeio(nubank)
        r.cadastros.salvarMeio(inter)

        inter.ativo = false
        r.cadastros.salvarMeio(inter)

        assertEquals(listOf(gabriel), r.cadastros.pessoas())
        assertEquals(listOf(nubankInst), r.cadastros.instituicoes())
        assertEquals(listOf<Any>(nubank), r.cadastros.meiosAtivos())
        inter.ativo = true
    }

    @Test
    fun `RN09 - possuiLancamentos indica se o meio tem gastos`() = runTest {
        val r = novosRepositorios()
        r.cadastros.salvarMeio(inter)
        r.cadastros.salvarMeio(nubank)
        r.lancamentos.salvar(gasto(1_000, LocalDate(2026, 11, 10)))

        assertTrue(r.cadastros.possuiLancamentos(inter))
        assertFalse(r.cadastros.possuiLancamentos(nubank))
    }

    // ---------- RecorrenciaRepository ----------

    @Test
    fun `recorrencias ativas e corretoras com aporte planejado`() = runTest {
        val r = novosRepositorios()
        val luz = ContaFixa("Luz", Dinheiro(20_000), 15, inter)
        val internet = ContaFixa("Internet", Dinheiro(10_000), 10, inter, ativa = false)
        val salario = ReceitaRecorrente("Salário", Dinheiro(800_000), 5)
        val clear = ContaInvestimento("Clear", gabriel, Instituicao("Clear"), Dinheiro(50_000))
        val binance = ContaInvestimento("Binance", gabriel, Instituicao("Binance"))
        r.recorrencias.salvarContaFixa(luz)
        r.recorrencias.salvarContaFixa(internet)
        r.recorrencias.salvarReceita(salario)
        r.cadastros.salvarMeio(clear)
        r.cadastros.salvarMeio(binance)

        assertEquals(listOf(luz), r.recorrencias.contasFixasAtivas())
        assertEquals(listOf(salario), r.recorrencias.receitasAtivas())
        assertEquals(listOf(clear), r.recorrencias.investimentosComAporte())
    }

    // ---------- ReembolsoRepository ----------

    @Test
    fun `reembolsos sao listados por terceiro`() = runTest {
        val r = novosRepositorios()
        val doPai = Reembolso(pai, Dinheiro(5_000), LocalDate(2026, 11, 20))
        r.reembolsos.salvar(doPai)
        r.reembolsos.salvar(Reembolso(Pessoa("Sogra", Papel.TERCEIRO), Dinheiro(1_000), LocalDate(2026, 11, 21)))

        assertEquals(listOf(doPai), r.reembolsos.deTerceiro(pai))
    }

    // ---------- Transacao ----------

    @Test
    fun `transacao grava tudo quando o bloco termina`() = runTest {
        val r = novosRepositorios()
        val resultado = r.transacao.executar {
            r.lancamentos.salvar(gasto(1_000, LocalDate(2026, 11, 10)))
            r.competencias.salvar(Competencia(novembro, SituacaoDaCompetencia.FECHADA))
            "ok"
        }

        assertEquals("ok", resultado)
        assertEquals(1, r.lancamentos.doMes(novembro).size)
        assertEquals(SituacaoDaCompetencia.FECHADA, r.competencias.buscarOuCriar(novembro).situacao)
    }

    @Test
    fun `transacao desfaz tudo quando o bloco falha no meio`() = runTest {
        val r = novosRepositorios()
        r.lancamentos.salvar(gasto(1_000, LocalDate(2026, 11, 1)))

        assertFailsWith<IllegalStateException> {
            r.transacao.executar {
                r.competencias.salvar(Competencia(novembro, SituacaoDaCompetencia.FECHADA))
                r.lancamentos.salvar(gasto(2_000, LocalDate(2026, 11, 2)))
                error("falha no meio do fechamento")
            }
        }

        assertEquals(listOf(Dinheiro(1_000)), r.lancamentos.doMes(novembro).map { it.valor })
        assertEquals(SituacaoDaCompetencia.ABERTA, r.competencias.buscarOuCriar(novembro).situacao)
    }
}
