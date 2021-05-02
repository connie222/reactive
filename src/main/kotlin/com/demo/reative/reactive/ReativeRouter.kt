package com.demo.reative.reactive

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class ReactiveRouter {
    val basePath = "/api/v1/reactive"

    @Bean
    fun reactiveRoute(handler: ReactiveHandler) = coRouter {
        path(basePath).nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("/{code}", handler::findByCode)
            }
        }
    }
}