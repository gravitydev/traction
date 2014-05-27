package com.gravitydev.traction

import scalaz._, syntax.validation._, syntax.applicative._
import com.typesafe.scalalogging.slf4j.Logging
import scala.reflect.api.TypeTags
import scala.reflect.runtime.universe._

sealed trait Decision
object Decision {
  trait Schedule  extends Decision
  trait CarryOn   extends Decision
  trait Fail      extends Decision
  trait Complete[T]  extends Decision {
    def result: T
  }
}

/** cake */
trait System {

  type WorkflowHistory

  /** Metadata about an activity. Implementation specific. */
  type ActivityMeta[A <: Activity[_,_]]
  /** Metadata about an workflow. Implementation specific. */
  type WorkflowMeta[T, W <: Workflow[T]]
 
  /** Workflow decisions */
  type Schedule <: Decision.Schedule
  type CarryOn  <: Decision.CarryOn // "Wait" is a bit overloaded
  type Fail     <: Decision.Fail
  type Complete[T] <: Decision.Complete[T]

  abstract class Workflow[T] {
    def flow: Step[T]
  }

  def carryOn: CarryOn

  def combineSchedules (a: Schedule, b: Schedule): Schedule

  def complete [T] (res: T): Complete[T]

  /**
   * Represents a step in the decision process
   */
  trait Step [T] extends Logging {
    /** Given the current history (state), decide what to do */
    def decide (state: WorkflowHistory, onSuccess: T => Decision, onFailure: String => Decision): Decision

    def map [X] (fn: T => X): Step[X] = new MappedStep(this, fn) // FIX

    def flatMap [X](fn: T => Step[X]) = new SequenceStep[T,X](this, fn)
  }

  object Step {
    def list [T](steps: List[Step[T]]) = new ParallelListSteps(steps)
  }


  implicit class Step1 [A] (s: Step[A]) extends MappedStep[A,A](s, identity) {
    def |~| [X](s: Step[X]): Step2[A,X] = new Step2 (new ParallelSteps(this, s))
  }
  class Step2 [A,B] (s: Step[(A,B)]) extends MappedStep[(A,B),(A,B)](s, identity) {
    def |~| [X](s: Step[X]): Step[(A,B,X)] = new ParallelSteps(this, s) map {case ((a,b),x) => (a,b,x)}
  }

  class MappedStep [T,X](step: Step[T], fn: T=>X) extends Step[X] with Logging {
    def decide (state: WorkflowHistory, onSuccess: X => Decision, onFailure: String => Decision) = 
      step.decide(
        state, 
        result => {
          logger.info("Result: " + result) 
          onSuccess(fn(result))
        },
        error => onFailure(error)
      )
  }

  class SequenceStep[A, B](first: Step[A], next: A => Step[B]) extends Step[B] {
    def decide (history: WorkflowHistory, onSuccess: B=>Decision, onFailure: String=>Decision) = {
      first.decide(
        history,
        res => {
          next(res)
            .decide(history, onSuccess, onFailure)
        },
        onFailure
      )
    }
  }

  class ParallelSteps [A, B] (step1: Step[A], step2: Step[B]) extends Step[(A,B)] {
    def decide (history: WorkflowHistory, onSuccess: ((A,B)) => Decision, onFailure: String => Decision) = {
      val res1 = step1.decide(
        history,
        res => complete(res),
        onFailure
      )

      val res2 = step2.decide(
        history,
        res => complete(res),
        onFailure
      )

      (res1, res2) match {
        case (a: Decision.Schedule, b: Decision.Schedule) => {
          logger.info("Scheduling parallel" + a + " and " + b) 
          combineSchedules(a.asInstanceOf[Schedule], b.asInstanceOf[Schedule])
        }
        case (a: Decision.Schedule, _) => a.asInstanceOf[Schedule]
        case (_, b: Decision.Schedule) => b.asInstanceOf[Schedule]
        case (_: Decision.CarryOn, _) => carryOn
        case (_, _:Decision.CarryOn) => carryOn
        case (a: Decision.Complete[_], b: Decision.Complete[_]) => onSuccess( (a.result.asInstanceOf[A], b.result.asInstanceOf[B]) )

        // TODO: handle failure
        case x => {
          logger.info("Unexpected status: " + x)
          ???
        }
      }
    }
  }

  class ParallelListSteps [A] (steps: List[Step[A]]) extends Step[List[A]] {
    def decide (history: WorkflowHistory, onSuccess: List[A] => Decision, onFailure: String => Decision) = {
      val decisions = steps map {s =>
        s.decide(history, res => complete(res), onFailure)
      }

      val schedules = decisions collect {
        case (a: Decision.Schedule) => a.asInstanceOf[Schedule]
      }

      val waiting = decisions collect {
        case (a: Decision.CarryOn) => a.asInstanceOf[CarryOn]
      }

      (schedules, waiting) match {
        case (head :: tail, _) => (head /: tail)(combineSchedules)
        case (_, head :: tail) => carryOn
        case (_, _) =>  
          (complete( List.empty[A] ) /: decisions) {(a,b) =>
            b match {
              case x : Decision.Complete[_] => complete(a.result.asInstanceOf[List[A]] ++ List(x.result.asInstanceOf[A]))

              // TODO: handle failure
              case x => {
                logger.info("Unexpected status: " + x)
                ???
              }
            }
          } match {
            case x: Decision.Complete[_] => onSuccess(x.result.asInstanceOf[List[A]])
            case x => {
              logger.info("Unexpected status: " + x)
              ???
            }
          }
      }
    }
  }

}

