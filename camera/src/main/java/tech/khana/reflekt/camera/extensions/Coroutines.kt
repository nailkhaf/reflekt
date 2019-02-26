package tech.khana.reflekt.camera.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend inline fun <reified T : Any> List<ReceiveChannel<T>>.combineLatest(
    scope: CoroutineScope
): ReceiveChannel<List<T>> =
    Channel<List<T>>(Channel.UNLIMITED).also { sendChannel ->
        val array = arrayOfNulls<T>(size)
        var readyCombine = false
        val mutex = Mutex()
        forEachIndexed { index, channel ->
            scope.launch {
                val receive = channel.receive()
                mutex.withLock {
                    array[index] = receive
                    if (readyCombine.not()) {
                        readyCombine = array.mapNotNull { it }.size == size
                    }
                    if (readyCombine) {
                        sendChannel.send(array.mapNotNull { it })
                    }
                }
            }
        }
    }