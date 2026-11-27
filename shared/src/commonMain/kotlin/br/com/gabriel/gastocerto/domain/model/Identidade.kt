package br.com.gabriel.gastocerto.domain.model

import kotlin.uuid.Uuid

/** D2 – identificador único (UUID) gerado no domínio, usado na igualdade das entidades. */
internal fun novoId(): String = Uuid.random().toString()
