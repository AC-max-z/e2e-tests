package ru.belkacar.core.test

import io.qameta.allure.LabelAnnotation
import java.lang.annotation.Inherited


// TODO: move me to
//  https://gitlab.belkacar.ru/java-backend/allure-test-tools
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@LabelAnnotation(name = "service-group")
annotation class ServiceGroup(val value: String)