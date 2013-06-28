package com.gravitydev.traction

import play.api.libs.json.Json
import amazonswf.ActivityMeta

trait Step [T] {
  def decide (state: List[ActivityState]): WorkflowDecision
  def stepNumber: Int

  def map [X] (fn: T => X): Step[X] = new MappedStep(this, fn) // FIX
  
  def withNumber(stepNum: Int): Step[T]
}

class MappedStep [T,X](step: Step[T], fn: T=>X) extends Step[X] {
  lazy val stepNumber = step.stepNumber
  def decide (state: List[ActivityState]) = step.decide(state)
  def withNumber(stepNum: Int) = new MappedStep(step.withNumber(stepNum), fn)
}

class SequenceInvocation[Z, A <: Activity[_,Z], B](first: ActivityInvocation[Z,A], b: Z => Step[B], val stepNumber: Int) extends Step[B] {
  private val a = first.withNumber(stepNumber)
  def decide (history: List[ActivityState]) = {
    a.status(history) map {
      case ActivityComplete(_, res) => res fold (
        error => FailWorkflow(error), 
        result => {
          b(a.withNumber(stepNumber).parseResult(result)).withNumber(stepNumber+1).decide(history)
        }
      )
      case ActivityInProcess(_) => WaitOnActivities
    } getOrElse ScheduleActivities(List(a.schedule))
  }
  def withNumber(stepNum: Int) = new SequenceInvocation(a, b, stepNum)
}

class ParallelInvocation [I,A<:Activity[_,I], J,B<:Activity[_,J]] (step1: ActivityInvocation[_,A], step2: ActivityInvocation[_,B], val stepNumber: Int) extends Step[(I,J)] {
  private val a = step1.withNumber(stepNumber)
  private val b = step2.withNumber(stepNumber+1)
  
  def decide (history: List[ActivityState]) = {
    val status1 = a.status(history)
    val status2 = b.status(history)
    
    // if neither has been started
    if (status1.isEmpty && status2.isEmpty) {
      ScheduleActivities(List(a.schedule, b.schedule))
    } else {
      (for (as <- status1; bs <- status2) yield {
        
      }) getOrElse WaitOnActivities
    }
    
    ???
  }
  
  def withNumber (stepNum: Int) = new ParallelInvocation (step1, step2, stepNum)
}

sealed trait ActivityState {
  def stepNumber: Int
}
case class ActivityInProcess (stepNumber: Int) extends ActivityState
case class ActivityComplete (stepNumber: Int, result: Either[String,String]) extends ActivityState

class ActivityInvocation [T, A <: Activity[_,T] : ActivityMeta] (
  val activity: A with Activity[_,T],
  val stepNumber: Int
) extends Step [T] {
  lazy val meta = implicitly[ActivityMeta[A]]
  
  def parseResult (res: String): T = activity.resultFormat.reads(Json.parse(res)).get
  
  def status (history: List[ActivityState]): Option[ActivityState] = history.find(_.stepNumber == stepNumber)
  
  def decide (history: List[ActivityState]) = status(history) map {
    case ActivityComplete(_, res) => res fold (error => FailWorkflow(error), result => CompleteWorkflow(result))
    case ActivityInProcess(_) => WaitOnActivities
  } getOrElse ScheduleActivities(List(ScheduleActivity(activity, stepNumber)))
  
  def schedule: ScheduleActivity[A with Activity[_,_]] = ScheduleActivity(activity, stepNumber)
  
  def flatMap [X](fn: T => Step[X]) = new SequenceInvocation[T,A,X](this, fn, stepNumber)
  
  def && [J,Z,Y <: Activity[J,Z]](i: Y with Activity[J,Z])(implicit ev: ActivityMeta[Y]) = new ParallelInvocation[T,A,Z,Y](this, new ActivityInvocation(i, stepNumber+1), stepNumber)
  
  def withNumber (stepNum: Int) = new ActivityInvocation(activity, stepNum)
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
