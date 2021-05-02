package com.demo.reative

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.asFlow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.tcp.TcpClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReativeApplicationTests(
   @Autowired private val objectMapper: ObjectMapper
) {
    @Test
    fun reactiveTestBasket() {
        val basket1 = mutableListOf("kiwi", "orange", "lemon", "orange", "lemon", "kiwi")
        val basket2 = mutableListOf("banana", "lemon", "lemon", "kiwi")
        val basket3 = mutableListOf("strawberry", "orange", "lemon", "grape", "strawberry")
        var baskets = mutableListOf(basket1, basket2, basket3)

        val basketFlux = Flux.fromIterable(baskets)
        basketFlux.concatMap { basket ->
            val distinctFruits = Flux.fromIterable(basket).distinct().collectList()
            val countFruits = Flux.fromIterable(basket)
                .groupBy { fruit -> fruit }
                .concatMap { groupedFlux ->
                    groupedFlux.count()
                        .map { count ->
                            val fruitCount = mutableMapOf<String, Long>()
                            fruitCount.put(groupedFlux.key(), count!!)
                            fruitCount
                        }
                }.reduce { acc, current ->
                    val map = mutableMapOf<String, Long>()
                    map.putAll(acc)
                    map.putAll(current)
                    map
                }
            Flux.zip(distinctFruits, countFruits) { distinct, count -> FruitInfo(distinct, count) }
        }.subscribe({
            println(it)
        }, {
            println("error")
        }, {
            println("complete")
        }
        )
    }

    @Test
    fun testByTcpClient() {
        //target쪽
        val connection = TcpClient.create()
            .host("127.0.0.1")
            .port(80)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .connectNow()

        val param = Mono.just("hello")
        connection.outbound().sendString(param).then().subscribe()
        val result = connection.inbound().receive().asString().flatMap { input ->
            Mono.just(
                ServerSentEvent.builder<String>()
                    .data(objectMapper.writeValueAsString(input))
                    .build()
            )
        }.doOnError { connection.disposeNow() }
            .doOnCancel {
                connection.disposeNow()
            }
            .doFinally { connection.disposeNow() }

        println("??? : "+ result.asFlow())
    }
}

data class FruitInfo(
    var distinctFruits: List<String>,
    val countFruits: MutableMap<String, Long>
)
