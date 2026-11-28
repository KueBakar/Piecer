package com.piecer.app.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Handler global untuk uncaught exception di coroutine (terdaftar via ServiceLoader).
 */
class GlobalCoroutineExceptionHandler :
    AbstractCoroutineContextElement(CoroutineExceptionHandler),
    CoroutineExceptionHandler {

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        CrashReporter.reportUncaughtException(
            thread = Thread.currentThread(),
            throwable = exception
        )
    }
}
