package com.gravitydev.traction.amazonswf

sealed trait ActivityEvent {
  def activityId: String
}
case class ActivityScheduled(activityId: String) extends ActivityEvent
case class ActivityStarted (activityId: String) extends ActivityEvent
case class ActivitySucceeded (activityId: String, result: String) extends ActivityEvent
case class ActivityFailed (activityId: String, reason: String) extends ActivityEvent

