package tech.khana.reflekt.ext

import kotlinx.coroutines.sync.Mutex


suspend fun Mutex.lockSelf() {
    lock()
    lock()
}
