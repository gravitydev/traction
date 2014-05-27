package com.gravitydev.traction

/**
 * An activity is a small unit of work that can succeed of fail as a whole
 * It is essentially a function that takes a context C as a parameter and produces an result R
 */
trait Activity [C, R] {
  def apply (ctx: C): R
}

