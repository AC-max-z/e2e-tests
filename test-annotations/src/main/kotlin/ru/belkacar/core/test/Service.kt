package ru.belkacar.core.test

import org.junit.jupiter.api.Tag

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("detector")
annotation class Detector()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("hash-preprocessor")
annotation class HashPreprocessor()
