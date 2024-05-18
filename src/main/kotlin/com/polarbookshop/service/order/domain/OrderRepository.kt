package com.polarbookshop.service.order.domain

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface OrderRepository : ReactiveCrudRepository<Order, Long> {

  fun findAllByCreatedBy(userId: String?): Flux<Order>
}
