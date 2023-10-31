package com.polarbookshop.service.order.config

import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "polar")
data class ClientProperties(

  @field:NotNull
  val catalogServiceUri: URI
)
