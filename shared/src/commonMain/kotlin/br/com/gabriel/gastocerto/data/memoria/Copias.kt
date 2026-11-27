package br.com.gabriel.gastocerto.data.memoria

import br.com.gabriel.gastocerto.domain.model.CarteiraMoedaEstrangeira
import br.com.gabriel.gastocerto.domain.model.CartaoDeCredito
import br.com.gabriel.gastocerto.domain.model.ContaCorrente
import br.com.gabriel.gastocerto.domain.model.ContaFixa
import br.com.gabriel.gastocerto.domain.model.ContaInvestimento
import br.com.gabriel.gastocerto.domain.model.Gasto
import br.com.gabriel.gastocerto.domain.model.Lancamento
import br.com.gabriel.gastocerto.domain.model.MeioDePagamento
import br.com.gabriel.gastocerto.domain.model.Parcela
import br.com.gabriel.gastocerto.domain.model.Receita
import br.com.gabriel.gastocerto.domain.model.ReceitaRecorrente

// Cópias dos objetos mutáveis do domínio, para o banco em memória se comportar como um banco
// de verdade (o mesmo papel dos mappers Entity ⇄ domínio do Room). Mesmo id, mesmos valores.

internal fun MeioDePagamento.copia(): MeioDePagamento = when (this) {
    is CartaoDeCredito -> CartaoDeCredito(nome, titular, instituicao, diaFechamento, diaVencimento, ativo, id)
    is ContaCorrente -> ContaCorrente(nome, titular, instituicao, ativo, id)
    is CarteiraMoedaEstrangeira -> CarteiraMoedaEstrangeira(nome, titular, instituicao, moeda, ativo, id)
    is ContaInvestimento -> ContaInvestimento(nome, titular, instituicao, aportePlanejadoMensal, ativo, id)
}

internal fun Lancamento.copia(): Lancamento = when (this) {
    is Parcela -> Parcela(
        valor, data, descricao, competencia, meio as CartaoDeCredito, compraId, numero, totalDeParcelas, terceiro, id,
    )
    is Gasto -> Gasto(valor, data, descricao, competencia, meio, terceiro, conversao, situacao, origemId, id)
    is Receita -> Receita(valor, data, descricao, competencia, tipo, situacao, origemId, id)
}

internal fun ContaFixa.copia() = ContaFixa(descricao, valorPrevisto, diaVencimento, meio, ativa, id)

internal fun ReceitaRecorrente.copia() = ReceitaRecorrente(descricao, valor, diaRecebimento, ativa, id)
