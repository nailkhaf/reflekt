package tech.khana.reflekt.api

import android.util.Log

interface Logger {

    val tag: String

    val logPrefix: String

    fun log(message: String, error: Throwable? = null)

    companion object : Logger {

        override val tag: String = "reflekt"

        override val logPrefix: String = ""

        override fun log(message: String, error: Throwable?) {
            if (error == null) {
                Log.d(tag, "$logPrefix $message")
            } else {
                Log.e(tag, "$logPrefix $message", error)
            }
        }
    }
}

fun Logger.debug(message: () -> String) = log(message())

fun Logger.error(error: Throwable) = log(error.message ?: "", error)
