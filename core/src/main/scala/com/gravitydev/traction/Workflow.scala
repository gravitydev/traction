package com.gravitydev.traction

import play.api.libs.json.{Json, Format}

abstract class Workflow[T : Format] {
  val format = implicitly[Format[T]]
  
  def serializeResult (value: T): String = Json.stringify(format.writes(value))
  
  def flow: Step[T]
}
