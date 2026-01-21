package com.insidehealthgt.hms.audit

import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Utility to access Spring ApplicationContext from non-Spring managed classes,
 * such as JPA Entity Listeners.
 */
@Component
class SpringContext : ApplicationContextAware {

    companion object {
        private val log = LoggerFactory.getLogger(SpringContext::class.java)
        private var context: ApplicationContext? = null

        fun <T : Any> getBean(beanClass: Class<T>): T {
            val ctx = context ?: throw IllegalStateException("ApplicationContext not initialized")
            return ctx.getBean(beanClass)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getBeanOrNull(beanClass: Class<T>): T? {
            val ctx = context ?: return null
            // ApplicationContext itself implements ApplicationEventPublisher
            return if (beanClass == ApplicationEventPublisher::class.java) {
                ctx as T
            } else {
                try {
                    ctx.getBean(beanClass)
                } catch (e: BeansException) {
                    log.debug("Bean {} not available: {}", beanClass.simpleName, e.message)
                    null
                }
            }
        }

        fun isInitialized(): Boolean = context != null
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
    }
}
