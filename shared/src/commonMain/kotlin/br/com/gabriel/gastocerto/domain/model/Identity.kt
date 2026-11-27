package br.com.gabriel.gastocerto.domain.model

import kotlin.uuid.Uuid

/** D2 – unique identifier (UUID) generated in the domain, used for entity equality. */
internal fun newId(): String = Uuid.random().toString()
