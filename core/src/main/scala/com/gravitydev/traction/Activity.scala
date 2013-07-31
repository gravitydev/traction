package com.gravitydev.traction

import play.api.libs.json.Format

abstract class Activity [C, T: Format] {
  def apply (ctx: C): T
  lazy val resultFormat = implicitly[Format[T]]
}
