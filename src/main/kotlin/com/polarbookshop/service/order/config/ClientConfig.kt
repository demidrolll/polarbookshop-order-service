package com.polarbookshop.service.order.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ClientConfig {

  @Bean
  fun webClient(
    properties: ClientProperties,
    builder: WebClient.Builder,
  ): WebClient =
    builder.baseUrl(properties.catalogServiceUri.toString())
      .build()
}
