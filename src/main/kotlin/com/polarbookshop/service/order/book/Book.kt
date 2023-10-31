package com.polarbookshop.service.order.book

data class Book(
  val isbn: String,
  val title: String,
  val author: String,
  val price: Double,
)
