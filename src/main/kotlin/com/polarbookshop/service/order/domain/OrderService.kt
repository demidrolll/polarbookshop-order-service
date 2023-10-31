package com.polarbookshop.service.order.domain

import com.polarbookshop.service.order.book.Book
import com.polarbookshop.service.order.book.BookClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class OrderService(
  private val orderRepository: OrderRepository,
  private val bookClient: BookClient,
) {

  fun getAllOrders(): Flux<Order> =
    orderRepository.findAll()

  fun submitOrder(isbn: String, quantity: Int) =
    bookClient.getBookByIsbn(isbn)
      .map { book -> buildAcceptedOrder(book, quantity) }
      .switchIfEmpty { Mono.just(buildRejectedOrder(isbn, quantity)) }
      .flatMap(orderRepository::save)

  companion object {

    fun buildRejectedOrder(bookIsbn: String, quantity: Int): Order =
      Order.of(
        bookIsbn = bookIsbn,
        bookName = null,
        bookPrice = null,
        quantity = quantity,
        orderStatus = OrderStatus.REJECTED
      )

    fun buildAcceptedOrder(book: Book, quantity: Int): Order =
      Order.of(
        bookIsbn = book.isbn,
        bookName = "${book.title} - ${book.author}",
        bookPrice = book.price,
        quantity = quantity,
        orderStatus = OrderStatus.ACCEPTED
      )
  }
}
