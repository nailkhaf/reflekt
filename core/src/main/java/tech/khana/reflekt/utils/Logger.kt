package tech.khana.reflekt.utils

import android.util.Log
import android.util.Log.*

const val REFLEKT_TAG = "reflekt"

interface Logger {

    val tag: String

    val level: Int

    fun log(level: Int, message: String, throwable: Throwable? = null)

    companion object {

        var defaultLogger = object : Logger {

            override val tag: String = REFLEKT_TAG

            override val level: Int = VERBOSE

            override fun log(level: Int, message: String, throwable: Throwable?) {
                if (throwable != null) {
                    Log.println(level, tag, message)
                } else {
                    Log.e(tag, message, throwable)
                }
            }
        }
    }
}

inline fun Logger.verbose(message: () -> String) = takeIf { suitLevel(VERBOSE) }?.run { log(VERBOSE, message()) }

inline fun Logger.debug(message: () -> String) = takeIf { suitLevel(DEBUG) }?.run { log(DEBUG, message()) }

inline fun Logger.info(message: () -> String) = takeIf { suitLevel(INFO) }?.run { log(INFO, message()) }

inline fun Logger.warn(message: () -> String) = takeIf { suitLevel(WARN) }?.run { log(WARN, message()) }

inline fun Logger.error(throwable: () -> Throwable) = takeIf { suitLevel(ERROR) }?.run { log(VERBOSE, "", throwable()) }

fun Logger.suitLevel(level: Int) = this.level <= level