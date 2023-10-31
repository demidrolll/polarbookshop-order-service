package com.polarbookshop.service.order.domain

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class OrderService(
  private val orderRepository: OrderRepository,
) {

  fun getAllOrders(): Flux<Order> =
    orderRepository.findAll()

  fun submitOrder(isbn: String, quantity: Int) =
    Mono
      .fromCallable {
        buildRejectedOrder(isbn, quantity)
      }
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
  }
}
