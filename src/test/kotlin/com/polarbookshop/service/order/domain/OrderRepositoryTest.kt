package com.polarbookshop.service.order.domain

import com.polarbookshop.service.order.config.DataConfig
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.kotlin.test.test

@DataR2dbcTest
@Import(DataConfig::class)
@Testcontainers
@Disabled
class OrderRepositoryTest {

  @Autowired
  private lateinit var orderRepository: OrderRepository

  @Test
  fun createRejectedOrder() {
    val rejectedOrder = OrderService.buildRejectedOrder("1234567890", 3)
    orderRepository.save(rejectedOrder)
      .test()
      .expectNextMatches { order ->
        order.status == OrderStatus.REJECTED
      }
      .verifyComplete()
  }

  companion object {
    val postgresql: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:alpine3.17"))

    @DynamicPropertySource
    @JvmStatic
    fun postgresqlProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.r2dbc.url", OrderRepositoryTest::r2dbcUrl)
      registry.add("spring.r2dbc.username", postgresql::getUsername)
      registry.add("spring.r2dbc.password", postgresql::getPassword)
      registry.add("spring.flyway.url", postgresql::getJdbcUrl)
    }

    private fun r2dbcUrl(): String {
      return String.format(
        "r2dbc:postgresql://%s:%s/%s",
        postgresql.host,
        postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
        postgresql.databaseName
      )
    }
  }
}
