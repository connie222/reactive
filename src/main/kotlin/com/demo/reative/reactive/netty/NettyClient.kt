package com.demo.reative.reactive.netty

import io.netty.channel.ChannelOption
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpClient

private val log = KotlinLogging.logger {}

@Component
class NettyClient {
    fun connect(): Connection  {
       val connection = TcpClient.create()
            .host("127.0.0.1")
            .port(80)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .connectNow()

        return connection
    }

    fun sendString(sendMsg: Mono<String>, connection: Connection) {
        connection.outbound().sendString(sendMsg).then()
            .subscribe()
    }

    fun disconnect(connection: Connection) {
        connection!!.disposeNow()
    }
}