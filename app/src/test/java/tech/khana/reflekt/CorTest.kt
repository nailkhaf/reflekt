package tech.khana.reflekt

import kotlinx.coroutines.*
import org.junit.Test

class CorTest {

    @Test
    fun tt() {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        scope.launch {

            rr()

            delay(200)

            println("#3 ${Thread.currentThread().name}")
        }

        Thread.sleep(1000)
        println("finish test")
    }

    suspend fun rr() {
        withContext(Dispatchers.Default) {
            launch {
                delay(10)
                println("#1 ${Thread.currentThread().name}")
                error("")
                delay(500)
                println("#2 ${Thread.currentThread().name}")
            }
        }
    }
}