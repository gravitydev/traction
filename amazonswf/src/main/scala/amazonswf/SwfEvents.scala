package com.gravitydev.traction

import com.amazonaws.services.simpleworkflow.model._

sealed abstract class SwfEvent [T](val name: String, parse: HistoryEvent => T) {
  def unapply (ev: HistoryEvent) = Option(ev) filter (_.getEventType == name) map parse
}

object SwfEvents {
  object WorkflowExecutionStarted extends SwfEvent("WorkflowExecutionStarted", _.getWorkflowExecutionStartedEventAttributes)
  object DecisionTaskStarted      extends SwfEvent("DecisionTaskStarted",      _.getDecisionTaskStartedEventAttributes) 
  object ActivityTaskScheduled    extends SwfEvent("ActivityTaskScheduled",    _.getActivityTaskScheduledEventAttributes) 
  object ActivityTaskStarted      extends SwfEvent("ActivityTaskStarted",      _.getActivityTaskStartedEventAttributes) 
  object ActivityTaskCompleted    extends SwfEvent("ActivityTaskCompleted",    _.getActivityTaskCompletedEventAttributes)
  object ActivityTaskFailed       extends SwfEvent("ActivityTaskFailed",       _.getActivityTaskFailedEventAttributes)
  
  def all = List(WorkflowExecutionStarted, ActivityTaskScheduled, ActivityTaskScheduled, DecisionTaskStarted)
  def forName (name: String) = all find (_.name == name) 
}
