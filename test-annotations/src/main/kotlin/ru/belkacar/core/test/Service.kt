package ru.belkacar.core.test

import io.qameta.allure.LabelAnnotation
import org.junit.jupiter.api.Tag
import java.lang.annotation.Inherited

// TODO: replace me with annotation from
//  https://gitlab.belkacar.ru/java-backend/allure-test-tools 2022.1 once it is stable
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@LabelAnnotation(name = "service")
annotation class Service(val value: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("detector")
annotation class Detector()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("hash-preprocessor")
annotation class HashPreprocessor()
