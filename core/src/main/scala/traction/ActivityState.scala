package com.gravitydev.traction

import scalaz._, syntax.validation._

sealed trait ActivityState {
  def stepNumber: Int
}
case class ActivityInProcess (stepNumber: Int) extends ActivityState
case class ActivityComplete (stepNumber: Int, result: String \/ String) extends ActivityState

