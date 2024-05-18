package com.polarbookshop.service.order.web

import com.polarbookshop.service.order.config.SecurityConfig
import com.polarbookshop.service.order.domain.Order
import com.polarbookshop.service.order.domain.OrderService
import com.polarbookshop.service.order.domain.OrderStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest
@Import(SecurityConfig::class)
class OrderControllerTest {

  @Autowired
  private lateinit var webClient: WebTestClient

  @MockBean
  private lateinit var orderService: OrderService

  @MockBean
  private lateinit var reactiveJwtDecoder: ReactiveJwtDecoder

  @Test
  fun `when book is not available then reject order`() {
    val orderRequest = OrderRequest(isbn = "1234567890", 3)
    val expectedOrder = OrderService.buildRejectedOrder(orderRequest.isbn, orderRequest.quantity)
    given(
      orderService.submitOrder(isbn = orderRequest.isbn, quantity = orderRequest.quantity)
    ).willReturn(Mono.just(expectedOrder))

    webClient
      .mutateWith(
        SecurityMockServerConfigurers
          .mockJwt()
          .authorities(SimpleGrantedAuthority("ROLE_customer"))
      )
      .post()
      .uri("/orders")
      .bodyValue(orderRequest)
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody(Order::class.java)
      .value { order ->
        assertThat(order).isNotNull
        assertThat(order.status).isEqualTo(OrderStatus.REJECTED)
      }
  }
}
