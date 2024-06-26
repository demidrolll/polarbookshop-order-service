package com.polarbookshop.service.order.domain

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("orders")
data class Order(
  @field:Id
  val id: Long? = null,
  val bookIsbn: String,
  val bookName: String?,
  val bookPrice: Double?,
  val quantity: Int,
  val status: OrderStatus,

  @field:CreatedDate
  val createdDate: Instant? = null,

  @field:LastModifiedDate
  val lastModifiedDate: Instant? = null,

  @field:Version
  val version: Int = 0,

  @field:CreatedBy
  val createdBy: String? = null,

  @field:LastModifiedBy
  val lastModifiedBy: String? = null
) {
  companion object {
    fun of(
      bookIsbn: String,
      bookName: String?,
      bookPrice: Double?,
      quantity: Int,
      orderStatus: OrderStatus
    ): Order =
      Order(
        id = null,
        bookIsbn = bookIsbn,
        bookName = bookName,
        bookPrice = bookPrice,
        quantity = quantity,
        status = orderStatus,
      )
  }
}
