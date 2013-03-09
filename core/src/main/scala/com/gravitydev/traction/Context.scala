package com.gravitydev.traction

class Context [C] (value: C) {
  def execute [A <: Activity[C,_]](a: A) = a(value)
}
