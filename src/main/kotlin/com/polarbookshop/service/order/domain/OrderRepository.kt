package com.polarbookshop.service.order.domain

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : ReactiveCrudRepository<Order, Long>
