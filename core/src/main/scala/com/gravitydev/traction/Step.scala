package com.gravitydev.traction

import play.api.libs.json.Json
import amazonswf.ActivityMeta

trait Step [T] {
  def decide (state: List[ActivityState]): WorkflowDecision
  def stepNumber: Int
  def map [X] (fn: T => X): Step[X] = new MappedStep(this, fn)
}

class MappedStep [T,X](step: Step[T], fn: T=>X) extends Step[X] {
  def stepNumber = step.stepNumber
  def decide (state: List[ActivityState]) = step.decide(state)
}

sealed trait ActivityEvent {
  def stepNumber: Int
}
case class ActivityStarted (stepNumber: Int) extends ActivityEvent
case class ActivitySucceeded (stepNumber: Int, result: String) extends ActivityEvent
case class ActivityFailed (stepNumber: Int, reason: String) extends ActivityEvent

sealed trait ActivityState {
  def stepNumber: Int
}
case class ActivityInProcess (stepNumber: Int) extends ActivityState
case class ActivityComplete (stepNumber: Int, result: Either[String,String]) extends ActivityState

case class ActivityInvocation [T, A <: Activity[_,T] : ActivityMeta] (number: Int, activity: A with Activity[_,T]) extends Step [T] {
  def decide (state: List[ActivityState]) = state find (_.stepNumber == number) map {
    case ActivityComplete(_, res) => res fold (error => FailWorkflow(error), result => CompleteWorkflow(result))
    case ActivityInProcess(_) => WaitOnActivities
  } getOrElse ScheduleActivities(List(ScheduleActivity(activity, stepNumber)))
  def stepNumber = number
}

trait WorkflowDecision
case class ScheduleActivity [A <: Activity[_,_] : ActivityMeta] (activity: A, stepNumber: Int) {
  val meta = implicitly[ActivityMeta[A]]
  lazy val id = meta.id(activity)
  lazy val input = stepNumber + ":" + Json.stringify(meta.format.writes(activity))
}
case class ScheduleActivities (activities: List[ScheduleActivity[_]]) extends WorkflowDecision
case object WaitOnActivities extends WorkflowDecision
case class CompleteWorkflow (result: String) extends WorkflowDecision
case class FailWorkflow (reason: String) extends WorkflowDecision
