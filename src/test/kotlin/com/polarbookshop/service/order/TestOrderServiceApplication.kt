package com.polarbookshop.service.order

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.polarbookshop.service.order.book.Book
import com.polarbookshop.service.order.book.BookClient
import com.polarbookshop.service.order.domain.Order
import com.polarbookshop.service.order.domain.OrderStatus
import com.polarbookshop.service.order.event.OrderAcceptedMessage
import com.polarbookshop.service.order.web.OrderRequest
import dasniko.testcontainers.keycloak.KeycloakContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("integration")
@Testcontainers
@Import(TestChannelBinderConfiguration::class)
class TestOrderServiceApplication(
  @Autowired
  private val webTestClient: WebTestClient,

  @Autowired
  private val output: OutputDestination,
) {

  private val objectMapper = jacksonObjectMapper()

  @MockBean
  private lateinit var bookClient: BookClient

  @Test
  fun contextLoads() {
  }

  @Test
  fun `when get own orders then return`() {
    val bookIsbn = "1234567899"
    val book = Book(bookIsbn, "Title", "Author", 9.90)
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book))
    val orderRequest = OrderRequest(bookIsbn, 1)

    val expectedOrder = webTestClient.post().uri("/orders")
      .headers { headers ->
        headers.setBearerAuth(bjornTokens.accessToken)
      }
      .bodyValue(orderRequest)
      .exchange()
      .expectStatus().is2xxSuccessful()
      .expectBody(Order::class.java).returnResult().responseBody

    assertThat(expectedOrder).isNotNull()
    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
      .isEqualTo(OrderAcceptedMessage(expectedOrder?.id!!))

    webTestClient.get().uri("/orders")
      .headers { headers ->
        headers.setBearerAuth(bjornTokens.accessToken)
      }
      .exchange()
      .expectStatus().is2xxSuccessful()
      .expectBodyList(Order::class.java)
      .value<WebTestClient.ListBodySpec<Order>> { orders ->
        val orderIds = orders.map(Order::id)
        assertThat(orderIds).contains(expectedOrder.id)
      }
  }

  @Test
  fun `when get orders for another user then not returned`() {
    val bookIsbn = "1234567899"
    val book = Book(bookIsbn, "Title", "Author", 9.90)
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book))
    val orderRequest = OrderRequest(bookIsbn, 1)

    val orderByBjorn = webTestClient.post().uri("/orders")
      .headers { headers ->
        headers.setBearerAuth(bjornTokens.accessToken)
      }
      .bodyValue(orderRequest)
      .exchange()
      .expectStatus().is2xxSuccessful()
      .expectBody(Order::class.java).returnResult().responseBody

    assertThat(orderByBjorn).isNotNull()
    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
      .isEqualTo(OrderAcceptedMessage(orderByBjorn?.id!!))

    val orderByIsabelle = webTestClient.post().uri("/orders")
      .headers { headers ->
        headers.setBearerAuth(isabelleTokens.accessToken)
      }
      .bodyValue(orderRequest)
      .exchange()
      .expectStatus().is2xxSuccessful()
      .expectBody(Order::class.java).returnResult().responseBody

    assertThat(orderByIsabelle).isNotNull()
    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
      .isEqualTo(OrderAcceptedMessage(orderByIsabelle?.id!!))

    webTestClient.get().uri("/orders")
      .headers { headers ->
        headers.setBearerAuth(bjornTokens.accessToken)
      }
      .exchange()
      .expectStatus().is2xxSuccessful()
      .expectBodyList(Order::class.java)
      .value<WebTestClient.ListBodySpec<Order>> { orders ->
        val orderIds = orders.map(Order::id)
        assertThat(orderIds).contains(orderByBjorn.id)
        assertThat(orderIds).doesNotContain(orderByIsabelle.id)
      }
  }

  fun `when post request and book not exists then order rejected`() {
    val bookIsbn = "1234567894"
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty())
    val orderRequest = OrderRequest(bookIsbn, 3)

    webTestClient.post().uri("/orders")
      .headers { headers ->
        headers.setBearerAuth(bjornTokens.accessToken)
      }
      .bodyValue(orderRequest)
      .exchange()
      .expectStatus().is2xxSuccessful()
      .expectBody(Order::class.java)
      .value { order ->
        assertThat(order.bookIsbn).isEqualTo(orderRequest.isbn)
        assertThat(order.quantity).isEqualTo(orderRequest.quantity)
        assertThat(order.status).isEqualTo(OrderStatus.REJECTED)
      }
  }

  companion object {
    private val postgresql: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:alpine3.17"))

    @DynamicPropertySource
    @JvmStatic
    fun postgresqlProperties(registry: DynamicPropertyRegistry) {
      postgresql.start()

      registry.add("spring.r2dbc.url", ::r2dbcUrl)
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

    @JvmStatic
    private val keycloakContainer: KeycloakContainer =
      KeycloakContainer("quay.io/keycloak/keycloak:24.0.3")
        .withRealmImportFiles("test-realm-config.json")

    @JvmStatic
    @DynamicPropertySource
    fun dynamicProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") {
        keycloakContainer.authServerUrl + "realms/PolarBookshop"
      }
    }

    private lateinit var bjornTokens: KeycloakToken
    private lateinit var isabelleTokens: KeycloakToken

    @JvmStatic
    @BeforeAll
    fun generateAccessTokens() {
      keycloakContainer.start()

      val webClient = WebClient.builder()
        .baseUrl("${keycloakContainer.authServerUrl}realms/PolarBookshop/protocol/openid-connect/token")
        .defaultHeader(
          HttpHeaders.CONTENT_TYPE,
          MediaType.APPLICATION_FORM_URLENCODED_VALUE
        )
        .build()

      isabelleTokens = authenticateWith("isabelle", "password", webClient)
      bjornTokens = authenticateWith("bjorn", "password", webClient)
    }

    private fun authenticateWith(username: String, password: String, webClient: WebClient): KeycloakToken {
      return webClient
        .post()
        .body(
          BodyInserters.fromFormData("grant_type", "password")
            .with("client_id", "polar-test")
            .with("username", username)
            .with("password", password)
        )
        .retrieve()
        .bodyToMono(KeycloakToken::class.java)
        .blockOptional()
        .orElseThrow()
    }

    data class KeycloakToken(
      @JsonProperty("access_token")
      var accessToken: String
    )
  }
}
