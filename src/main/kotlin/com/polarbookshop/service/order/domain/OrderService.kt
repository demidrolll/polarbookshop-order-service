package com.polarbookshop.service.order.domain

import com.polarbookshop.service.order.book.Book
import com.polarbookshop.service.order.book.BookClient
import com.polarbookshop.service.order.event.OrderAcceptedMessage
import com.polarbookshop.service.order.event.OrderDispatchedMessage
import org.apache.logging.log4j.LogManager
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class OrderService(
  private val orderRepository: OrderRepository,
  private val bookClient: BookClient,
  private val streamBridge: StreamBridge
) {

  fun getAllOrders(): Flux<Order> =
    orderRepository.findAll()

  @Transactional
  fun submitOrder(isbn: String, quantity: Int) =
    bookClient.getBookByIsbn(isbn)
      .map { book -> buildAcceptedOrder(book, quantity) }
      .switchIfEmpty { Mono.just(buildRejectedOrder(isbn, quantity)) }
      .flatMap(orderRepository::save)
      .doOnNext(::publishOrderAcceptedEvent)

  fun consumeOrderDispatchedMessageEvent(flux: Flux<OrderDispatchedMessage>): Flux<Order> =
    flux
      .flatMap { message ->
        orderRepository.findById(message.orderId)
      }
      .map(::buildDispatchedOrder)
      .flatMap(orderRepository::save)

  private fun buildDispatchedOrder(exist: Order): Order =
    exist.copy(
      status = OrderStatus.DISPATCHED
    )

  private fun publishOrderAcceptedEvent(order: Order) {
    val orderAcceptedMessage = OrderAcceptedMessage(order.id ?: 0)
    logger.info("Sending order accepted event with id: {}", order.id)
    val result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage)
    logger.info("Result of sending data for order with id {}: {}", order.id, result);
  }

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

    private val logger = LogManager.getLogger()
  }
}
