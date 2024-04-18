package com.polarbookshop.service.order.event

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.polarbookshop.service.order.book.Book
import com.polarbookshop.service.order.book.BookClient
import com.polarbookshop.service.order.domain.Order
import com.polarbookshop.service.order.domain.OrderStatus
import com.polarbookshop.service.order.web.OrderRequest
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration::class)
@Testcontainers
class OrderServiceMessagingTests(
  @Autowired
  private val output: OutputDestination,
  @Autowired
  private val webTestClient: WebTestClient,
) {

  private val objectMapper = jacksonObjectMapper()

  @MockBean
  private lateinit var bookClient: BookClient

  @Test
  fun `when POST request and book exists then order accepted`() {
    val bookIsbn = "1234567899"
    val book = Book(bookIsbn, "Title", "Author", 9.90)
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book))
    val orderRequest = OrderRequest(bookIsbn, 3)

    val createdOrder = webTestClient.post().uri("/orders")
      .bodyValue(orderRequest)
      .exchange()
      .expectStatus().is2xxSuccessful()
      .expectBody(Order::class.java)
      .returnResult().responseBody

    assertThat(createdOrder).isNotNull()
    assertThat(createdOrder?.bookIsbn).isEqualTo(orderRequest.isbn)
    assertThat(createdOrder?.quantity).isEqualTo(orderRequest.quantity)
    assertThat(createdOrder?.bookName).isEqualTo(book.title + " - " + book.author)
    assertThat(createdOrder?.bookPrice).isEqualTo(book.price)
    assertThat(createdOrder?.status).isEqualTo(OrderStatus.ACCEPTED)

    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
      .isEqualTo(OrderAcceptedMessage(createdOrder?.id ?: 0))
  }
}
