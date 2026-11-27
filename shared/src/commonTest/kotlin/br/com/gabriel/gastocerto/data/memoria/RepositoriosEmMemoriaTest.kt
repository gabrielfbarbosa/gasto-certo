package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.repositorio.Repositorios
import br.com.gabriel.gastocerto.domain.repositorio.RepositoriosContrato

/** Os repositórios em memória (Web e testes) cumprem o contrato. */
class RepositoriosEmMemoriaTest : RepositoriosContrato() {
    override fun novosRepositorios(): Repositorios {
        val banco = BancoEmMemoria()
        return Repositorios(
            lancamentos = LancamentoEmMemoriaRepository(banco),
            competencias = CompetenciaEmMemoriaRepository(banco),
            cadastros = CadastroEmMemoriaRepository(banco),
            recorrencias = RecorrenciaEmMemoriaRepository(banco),
            reembolsos = ReembolsoEmMemoriaRepository(banco),
            transacao = TransacaoEmMemoria(banco),
        )
    }
}
