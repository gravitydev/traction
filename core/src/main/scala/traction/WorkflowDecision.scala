package com.gravitydev.traction

/*
sealed trait WorkflowDecision

class ScheduleActivity (val id: String, input: String) extends WorkflowDecision
object ScheduleActivity {
  def apply [A <: Activity[_,_] : ActivityMeta : Serializer] (activity: A, stepNumber: Int) = {
    val meta = implicitly[ActivityMeta[A]]
    new ScheduleActivity(
      id = meta.id(activity),
      input = stepNumber + ":" + Serializer[A].serialize(activity)
    )
  }
}

case class ScheduleActivities (activities: List[ScheduleActivity]) extends WorkflowDecision
case object WaitOnActivities extends WorkflowDecision
case class CompleteWorkflow (result: String) extends WorkflowDecision
case class FailWorkflow (reason: String) extends WorkflowDecision
*/

