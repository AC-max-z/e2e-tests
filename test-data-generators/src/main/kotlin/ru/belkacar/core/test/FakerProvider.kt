package ru.belkacar.core.test

import io.github.serpro69.kfaker.faker

object FakerProvider {

    val faker by lazy {
        faker { }
    }
}
