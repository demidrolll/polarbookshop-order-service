package com.polarbookshop.service.order.web

import com.polarbookshop.service.order.domain.Order
import com.polarbookshop.service.order.domain.OrderService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("orders")
class OrderController(
  private val orderService: OrderService
) {

  @GetMapping
  fun getAllOrders(@AuthenticationPrincipal jwt: Jwt): Flux<Order> =
    orderService.getAllOrders(jwt.subject)

  @PostMapping
  fun submitOrder(@RequestBody @Valid orderRequest: OrderRequest): Mono<Order> =
    orderService.submitOrder(isbn = orderRequest.isbn, quantity = orderRequest.quantity)
}
