package com.gravitydev.traction

trait Workflow[T] {
  def flow: Step[_]
}
