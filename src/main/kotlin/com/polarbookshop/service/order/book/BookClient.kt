package com.polarbookshop.service.order.book

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class BookClient(
  private val webClient: WebClient
) {

  fun getBookByIsbn(isbn: String): Mono<Book> =
    webClient.get()
      .uri("/books/$isbn")
      .retrieve()
      .bodyToMono(Book::class.java)
      .onErrorResume(WebClientResponseException::class.java) { ex ->
        if (ex.statusCode == HttpStatus.NOT_FOUND)
          Mono.empty()
        else
          Mono.error(ex)
      }
}
