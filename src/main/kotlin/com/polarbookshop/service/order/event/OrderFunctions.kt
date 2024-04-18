package com.polarbookshop.service.order.event

import com.polarbookshop.service.order.domain.OrderService
import org.apache.logging.log4j.LogManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import java.util.function.Consumer

@Configuration
class OrderFunctions {

  @Bean
  fun dispatchOrder(orderService: OrderService): Consumer<Flux<OrderDispatchedMessage>> = Consumer { flux ->
    orderService.consumeOrderDispatchedMessageEvent(flux)
      .doOnNext { order ->
        logger.info("The order with id {} is dispatched", order.id)
      }
      .subscribe()
  }

  companion object {
    private val logger = LogManager.getLogger()
  }
}
