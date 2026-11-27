package br.com.gabriel.gastocerto.domain.model

import br.com.gabriel.gastocerto.domain.regra.DiaInvalido
import br.com.gabriel.gastocerto.domain.regra.ValorInvalido
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecorrenciasTest {

    private val gabriel = Pessoa("Gabriel", Papel.USUARIO)
    private val inter = ContaCorrente("Inter", gabriel, Instituicao("Inter"))
    private val novembro = MesAno(2026, 11)

    // ---------- RN06: contas fixas ----------

    @Test
    fun `RN06 - conta fixa gera gasto previsto no dia de vencimento`() {
        val luz = ContaFixa("Luz", Dinheiro(20_000), diaVencimento = 15, meio = inter)

        val previsto = luz.gerarPrevisto(novembro)

        assertEquals(LocalDate(2026, 11, 15), previsto.data)
        assertEquals(novembro, previsto.competencia)
        assertEquals(Dinheiro(20_000), previsto.valor)
        assertEquals(SituacaoDoLancamento.PREVISTO, previsto.situacao)
        assertEquals(inter, previsto.meio)
        assertEquals("Luz", previsto.descricao)
    }

    @Test
    fun `RN06 - vencimento em dia inexistente vira o ultimo dia do mes`() {
        val aluguel = ContaFixa("Aluguel", Dinheiro(150_000), diaVencimento = 31, meio = inter)
        assertEquals(LocalDate(2026, 11, 30), aluguel.gerarPrevisto(novembro).data)
        assertEquals(LocalDate(2027, 2, 28), aluguel.gerarPrevisto(MesAno(2027, 2)).data)
    }

    @Test
    fun `RN06 e D3 - previsto guarda a conta fixa de origem`() {
        val luz = ContaFixa("Luz", Dinheiro(20_000), 15, inter)
        assertEquals(luz.id, luz.gerarPrevisto(novembro).origemId)
        assertEquals(luz.id, luz.gerarPrevisto(novembro.proximo()).origemId)
    }

    @Test
    fun `RN06 - previsto confirmado assume o valor real do mes`() {
        val luz = ContaFixa("Luz", Dinheiro(20_000), 15, inter).gerarPrevisto(novembro)
        luz.confirmar(Dinheiro(18_437))
        assertEquals(Dinheiro(18_437), luz.valor)
        assertEquals(SituacaoDoLancamento.REALIZADO, luz.situacao)
    }

    @Test
    fun `RN06 e RN09 - conta fixa com valor zero ou dia invalido e rejeitada`() {
        assertFailsWith<ValorInvalido> { ContaFixa("Luz", Dinheiro(0), 15, inter) }
        assertFailsWith<DiaInvalido> { ContaFixa("Luz", Dinheiro(20_000), 0, inter) }
        assertFailsWith<DiaInvalido> { ContaFixa("Luz", Dinheiro(20_000), 32, inter) }
    }

    // ---------- RN07: salário ----------

    @Test
    fun `RN07 - salario gera receita prevista no dia de recebimento`() {
        val salario = ReceitaRecorrente("Salário", Dinheiro(800_000), diaRecebimento = 5)

        val prevista = salario.gerarPrevista(novembro)

        assertEquals(LocalDate(2026, 11, 5), prevista.data)
        assertEquals(novembro, prevista.competencia)
        assertEquals(TipoDeReceita.SALARIO, prevista.tipo)
        assertEquals(SituacaoDoLancamento.PREVISTO, prevista.situacao)
        assertEquals(salario.id, prevista.origemId)
    }

    @Test
    fun `RN07 - recebimento dia 30 em fevereiro vira o ultimo dia`() {
        val salario = ReceitaRecorrente("Salário", Dinheiro(800_000), diaRecebimento = 30)
        assertEquals(LocalDate(2028, 2, 29), salario.gerarPrevista(MesAno(2028, 2)).data)
    }

    @Test
    fun `RN07 e RN09 - salario com valor zero ou dia invalido e rejeitado`() {
        assertFailsWith<ValorInvalido> { ReceitaRecorrente("Salário", Dinheiro(0), 5) }
        assertFailsWith<DiaInvalido> { ReceitaRecorrente("Salário", Dinheiro(800_000), 40) }
    }

    // ---------- RN11: aporte planejado ----------

    @Test
    fun `RN11 - corretora com aporte planejado gera aporte previsto no mes`() {
        val clear = ContaInvestimento("Clear", gabriel, Instituicao("Clear"), aportePlanejadoMensal = Dinheiro(50_000))

        val aporte = assertNotNull(clear.gerarAportePrevisto(novembro))

        assertEquals(Dinheiro(50_000), aporte.valor)
        assertEquals(LocalDate(2026, 11, 1), aporte.data)
        assertEquals(novembro, aporte.competencia)
        assertEquals(clear, aporte.meio)
        assertEquals(SituacaoDoLancamento.PREVISTO, aporte.situacao)
        assertEquals(clear.id, aporte.origemId)
    }

    @Test
    fun `RN11 - corretora sem aporte planejado nao gera previsto`() {
        assertNull(ContaInvestimento("Binance", gabriel, Instituicao("Binance")).gerarAportePrevisto(novembro))
    }
}
