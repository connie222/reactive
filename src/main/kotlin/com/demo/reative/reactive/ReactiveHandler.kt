package com.demo.reative.reactive

import com.demo.reative.reactive.netty.NettyClient
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Component
class ReactiveHandler(
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val nettyClient: NettyClient
) {
    suspend fun findByCode(request: ServerRequest): ServerResponse {
        log.info { "]-----] ReactiveHandler::findByCode call [-----[ " }
        val code = request.pathVariable("code")
        val connection = nettyClient.connect()

        log.debug { "]-----] ReactiveHandler::connection [-----[ " }

        val param = Mono.just(code)
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
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValueAndAwait(code)
    }
}