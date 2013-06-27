package com.gravitydev.traction

import play.api.libs.json.Format

abstract class Workflow[T : Format] {
  def flow: Step[T]
}
