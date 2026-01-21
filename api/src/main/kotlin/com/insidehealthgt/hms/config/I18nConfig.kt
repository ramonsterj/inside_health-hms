package com.insidehealthgt.hms.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import java.util.Locale

@Configuration
class I18nConfig : WebMvcConfigurer {

    companion object {
        val SUPPORTED_LOCALES: List<Locale> = listOf(Locale.ENGLISH, Locale.of("es"))
        val DEFAULT_LOCALE: Locale = Locale.ENGLISH
        private const val MESSAGE_CACHE_SECONDS = 3600
    }

    @Bean
    fun messageSource(): MessageSource = ReloadableResourceBundleMessageSource().apply {
        setBasenames(
            "classpath:messages/messages",
            "classpath:messages/validation",
            "classpath:messages/errors",
        )
        setDefaultEncoding("UTF-8")
        setUseCodeAsDefaultMessage(true)
        setCacheSeconds(MESSAGE_CACHE_SECONDS)
    }

    @Bean
    fun localeResolver(): LocaleResolver = AcceptHeaderLocaleResolver().apply {
        supportedLocales = SUPPORTED_LOCALES
        setDefaultLocale(DEFAULT_LOCALE)
    }

    @Bean
    fun localeChangeInterceptor(): LocaleChangeInterceptor = LocaleChangeInterceptor().apply {
        paramName = "lang"
    }

    @Bean
    fun validator(messageSource: MessageSource): LocalValidatorFactoryBean = LocalValidatorFactoryBean().apply {
        setValidationMessageSource(messageSource)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }
}
