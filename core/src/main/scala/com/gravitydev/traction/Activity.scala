package com.gravitydev.traction

trait Activity [C, T] {
  def apply (ctx: C): T
}
