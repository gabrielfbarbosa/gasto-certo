package br.com.gabriel.gastocerto

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform