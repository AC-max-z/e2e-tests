package ru.belkacar.core

import org.slf4j.Logger
import reactor.core.publisher.Mono
import java.lang.Exception
import java.time.Duration

class Common {
    inline fun <T> executeWithRetry(
        retries: Int = 1,
        callFunc: () -> T?,
        logger: Logger,
        predicate: (T?) -> Boolean = { true }
    ): T? {
        var attempt = 0
        var result: T? = null
        while (attempt < retries && result == null) {
            try {
                result = callFunc()
                if (!predicate(result)) result = null
                attempt++
            } catch (e: Exception) {
                logger.error("Got exception while executing ${object {}.javaClass.enclosingMethod.name}")
                logger.error(e.message)
                attempt++
                if (attempt == retries) throw e
                else {
                    Mono.delay(Duration.ofMillis(DELAY_RETRY_MS)).block()!!
                    logger.info("Retrying. Attempt $attempt of $retries")
                }
            }
        }
        return result
    }
}