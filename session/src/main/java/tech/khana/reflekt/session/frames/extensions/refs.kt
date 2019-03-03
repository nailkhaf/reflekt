package tech.khana.reflekt.session.frames.extensions

import java.lang.ref.WeakReference

fun <T : Any> weak(value: T) = WeakReference<T>(value)