package com.polarbookshop.service.order.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache

@EnableWebFluxSecurity
@Configuration
class SecurityConfig {

  @Bean
  fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
    http
      .authorizeExchange {
        it.anyExchange().authenticated()
      }
      .oauth2ResourceServer {
        it.jwt(Customizer.withDefaults())
      }
      .requestCache {
        it.requestCache(NoOpServerRequestCache.getInstance())
      }
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .build()
}
