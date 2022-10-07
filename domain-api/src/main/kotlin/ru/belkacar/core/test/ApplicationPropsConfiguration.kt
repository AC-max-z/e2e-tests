package ru.belkacar.core.test

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [TelematicsPlatformProperties::class])
internal class ApplicationPropsConfiguration {
}