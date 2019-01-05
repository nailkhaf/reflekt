package tech.khana.reflekt.core

import android.util.Log
import tech.khana.reflekt.core.LogLevel.*

internal val cameraLogger = object : Tag {

    override val tag: String = "ReflektCamera"

    override val level: LogLevel = LogLevel.DEFAULT
}

interface Logger {

    fun v(tag: String, message: String)

    fun d(tag: String, message: String)

    fun i(tag: String, message: String)

    fun w(tag: String, message: String)

    fun e(tag: String, throwable: Throwable)

    companion object {

        var logger = object : Logger {

            override fun v(tag: String, message: String) {
                Log.v(tag, message)
            }

            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun i(tag: String, message: String) {
                Log.i(tag, message)
            }

            override fun w(tag: String, message: String) {
                Log.w(tag, message)
            }

            override fun e(tag: String, throwable: Throwable) {
                Log.e(tag, "", throwable)
            }
        }
    }
}

interface Tag {

    val tag: String

    val level: LogLevel
}

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    companion object {
        val DEFAULT = if (BuildConfig.DEBUG) DEBUG else WARN
    }
}

inline fun Tag.verbose(message: () -> String) =
    if (suitLevel(VERBOSE)) Logger.logger.v(tag, message()) else Unit

inline fun Tag.debug(message: () -> String) =
    if (suitLevel(DEBUG)) Logger.logger.d(tag, message()) else Unit

inline fun Tag.info(message: () -> String) =
    if (suitLevel(INFO)) Logger.logger.i(tag, message()) else Unit

inline fun Tag.warn(message: () -> String) =
    if (suitLevel(WARN)) Logger.logger.w(tag, message()) else Unit

inline fun Tag.error(throwable: () -> Throwable) =
    if (suitLevel(ERROR)) Logger.logger.e(tag, throwable()) else Unit

fun Tag.suitLevel(level: LogLevel) = this.level.ordinal <= level.ordinal