package com.gravitydev.traction

import scalaz._, syntax.validation._, syntax.applicative._

trait Decision

/** cake */
trait System {
  /** Metadata about an activity. Implementation specific. */
  type ActivityMeta[A <: Activity[_,_]]
  type WorkflowMeta[T, W <: Workflow[T]]

  type ActivityData
  type WorkflowData

  type Schedule <: Decision
  type CarryOn  <: Decision // wait is a bit overloaded
  type Fail     <: Decision
  type Complete <: Decision

  abstract class Workflow[T] {
    def flow: Step[T]
  }

  def carryOn: CarryOn
  //def activityData [T, A <: Activity[_,T]](activity: A, step: Int)(implicit meta: ActivityMeta[T,A]): ActivityData // = new ScheduleActivity(meta, meta.id(a), step)

  def schedule (activities: List[ActivityData]): Schedule

  /**
   * Represents a step in the decision process
   */
  sealed trait Step [T] {
    /** Given the current history (state), decide what to do */
    def decide (state: List[ActivityState], onSuccess: T => Decision, onFailure: String => Decision, stepNumber: Int = 1): Decision

    def map [X] (fn: T => X): Step[X] = new MappedStep(this, fn) // FIX

    //def parseResult (data: String): T
  }

  class MappedStep [T,X](step: Step[T], fn: T=>X) extends Step[X] {
    def decide (state: List[ActivityState], onSuccess: X => Decision, onFailure: String => Decision, stepNumber: Int) = 
      step.decide(
        state, 
        result => onSuccess(fn(result)),
        error => onFailure(error),
        stepNumber
      )

    //def parseResult (data: String) = fn(step.parseResult(data))
  }

  class SequenceStep[A, B](first: Step[A], next: A => Step[B]) extends Step[B] {
    def decide (history: List[ActivityState], onSuccess: B=>Decision, onFailure: String=>Decision, stepNumber: Int) = {
      first.decide(
        history,
        res => next(res).decide(history, onSuccess, onFailure, stepNumber+1),
        onFailure,
        stepNumber
      )
    }

    //def parseResult (data: String) = ???
  }

  /*
  class ParallelStep [I,A<:Activity[_,I], J,B<:Activity[_,J]] (step1: ActivityStep[I,A], step2: ActivityStep[J,B], val stepNumber: Int) extends Step[(I,J)] {
    def decide (history: List[ActivityState], onSuccess: ((I,J)) => Decision, onFailure: String => Decision): Decision = {
      val status1 = history.find(_.stepNumber == stepNumber)
      val status2 = history.find(_.stepNumber == stepNumber+1)
      
      // if neither has been started
      if (status1.isEmpty && status2.isEmpty) {
        // start them both
        schedule(step1.schedule ++ step2.schedule)
        
      // otherwise either collect their statuses or wait some more
      } else {
        (for (as <- status1; bs <- status2) yield {
          (as,bs) match {
            // scalaz this shit
            case (ActivityComplete(_,ar), ActivityComplete(_,br)) => ((ar.validation.toValidationNel |@| br.validation.toValidationNel) {(ra, rb) =>
              onSuccess(a.parseResult(ra) -> b.parseResult(rb)) 
            } valueOr {e => onFailure(e.list.mkString("; "))})
            case x => WaitOnActivities
          }
        }) getOrElse WaitOnActivities
      }
    }  
  }
  */

  class ActivityStep [T, A <: Activity[_,T]] (
    val activity: A with Activity[_,T]
  )(implicit meta: ActivityMeta[A]) extends Step [T] {
    //def parseResult (res: String): T = Serializer[T].unserialize(res)
    //def serializeResult (res: T): String = Serializer[T].serialize(res)
    
    def decide (history: List[ActivityState], onSuccess: T=>Decision, onFailure: String=>Decision, stepNumber: Int) = ??? /*history.find(_.stepNumber == stepNumber) map {
      case ActivityComplete(_, res) => res fold (error => onFailure(error), result => onSuccess(parseResult(result))) : Decision
      case ActivityInProcess(_) => carryOn 
    } getOrElse schedule(List(activityData(activity, stepNumber))) */
    
    //def schedule: List[ScheduleActivity] = List(ScheduleActivity(activity, stepNumber))
    
    def flatMap [X](fn: T => Step[X]) = new SequenceStep[T,X](this, fn)
    
    //def && [J,Z : Serializer, Y <: Activity[J,Z] : ActivityMeta : Serializer](i: Y with Activity[J,Z]) = new ParallelStep[T,A,Z,Y](this, new ActivityStep(i, stepNumber+1), stepNumber)
    
  }

}

