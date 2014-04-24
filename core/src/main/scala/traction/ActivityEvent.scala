package com.gravitydev.traction

sealed trait ActivityEvent {
  def stepNumber: Int
}
case class ActivityStarted (stepNumber: Int) extends ActivityEvent
case class ActivitySucceeded (stepNumber: Int, result: String) extends ActivityEvent
case class ActivityFailed (stepNumber: Int, reason: String) extends ActivityEvent
