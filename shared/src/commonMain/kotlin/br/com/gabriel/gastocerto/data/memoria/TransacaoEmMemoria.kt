package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.repositorio.Transacao

/** Tudo ou nada em memória: tira uma foto do [banco] e a restaura se o bloco lançar exceção. */
class TransacaoEmMemoria(private val banco: BancoEmMemoria) : Transacao {

    override suspend fun <T> executar(bloco: suspend () -> T): T {
        val foto = banco.foto()
        try {
            return bloco()
        } catch (erro: Throwable) {
            banco.restaurar(foto)
            throw erro
        }
    }
}
