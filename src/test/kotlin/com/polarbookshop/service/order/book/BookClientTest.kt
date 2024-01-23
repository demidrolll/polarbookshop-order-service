package com.polarbookshop.service.order.book

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.test.test

class BookClientTest {

  private lateinit var mockWebServer: MockWebServer
  private lateinit var bookClient: BookClient

  @BeforeEach
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()
    val webClient = WebClient.builder()
      .baseUrl(mockWebServer.url("/").toUri().toString())
      .build()
    bookClient = BookClient(webClient)
  }

  @AfterEach
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun whenBookExistsThenReturnBook() {
    val bookIsbn = "1234567890";
    val mockResponse = MockResponse()
      .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .setBody(
        """
{
"isbn": %s,
"title": "Title",
"author": "Author",
"price": 9.90,
"publisher": "Polarsophia"
}
"""
          .format(bookIsbn)
      )

    mockWebServer.enqueue(mockResponse)
    bookClient.getBookByIsbn(bookIsbn)
      .test()
      .expectNextMatches { book ->
        book.isbn.equals(bookIsbn)
      }
      .verifyComplete()
  }
}
