package tech.khana.reflekt.api

import android.util.Log

interface Logger {

    val tag: String

    val logPrefix: String
        get() = ""

    fun log(message: String, error: Throwable? = null)

    companion object : Logger {

        override val tag: String = "reflekt"

        override fun log(message: String, error: Throwable?) {
            if (error == null) {
                Log.d(tag, message)
            } else {
                Log.e(tag, message, error)
            }
        }
    }
}

fun Logger.debug(message: () -> String) =
    log(logPrefix + " " + message())

fun Logger.error(error: Throwable) =
    log(logPrefix + (error.message ?: ""), error)
