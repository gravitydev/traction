package com.gravitydev.traction

import scala.language.implicitConversions
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.runtime.universe._
import com.typesafe.scalalogging.slf4j.Logging
import scalaz._, syntax.validation._

package amazonswf {
  private [amazonswf] case class SwfActivityData [A <: Activity[_,_]](val activity: A, step: Int)(implicit val meta: SwfActivityMeta[_,A]) {
    lazy val id = meta.id(activity)
    lazy val input = step + ":" + meta.serializeActivity(activity)

    override def toString = "SwfActivityData(" + id + ": " + input + ")"
  }

  case class ScheduleActivities (activities: List[SwfActivityData[_]]) extends Decision.Schedule

  case object WaitOnActivities extends Decision.CarryOn
  case class CompleteWorkflow [T] (result: T) extends Decision.Complete[T]
  case class FailWorkflow (reason: String) extends Decision.Fail

  sealed trait ActivityState {
    def stepNumber: Int
  }
  case class ActivityInProcess (stepNumber: Int) extends ActivityState
  case class ActivityComplete (stepNumber: Int, result: String \/ String) extends ActivityState


  class ActivityStep [T, A <: Activity[_,T]] (
    val activity: A with Activity[_,T]
  )(implicit meta: SwfActivityMeta[T,A]) extends Step [T] {

    def decide (history: List[ActivityState], onSuccess: T=>Decision, onFailure: String=>Decision, stepNumber: Int): Decision = history.find(_.stepNumber == stepNumber) map {
      case ActivityComplete(_, res: \/[String,String]) => res fold (
        error => onFailure(error), 
        result => onSuccess( meta.parseResult(result) )
      ) : Decision
      case ActivityInProcess(_) => carryOn 
    } getOrElse ( ScheduleActivities( List(new SwfActivityData(activity, stepNumber)) ): Decision)
    
  }
}

abstract class Check[T] {
  def run: T
}

package object amazonswf extends System {
  type WorkflowHistory = List[ActivityState]
  type ActivityMeta[A <: Activity[_,_]] = SwfActivityMeta[_,A]
  type WorkflowMeta[T, W <: Workflow[T]] = SwfWorkflowMeta[T,W]

  type Complete[T] = CompleteWorkflow[T]
  type CarryOn = WaitOnActivities.type
  type Schedule = ScheduleActivities 

  def combineSchedules(a: ScheduleActivities, b: ScheduleActivities) = {
    ScheduleActivities(a.activities ++ b.activities)
  }

  def activityMeta [A <: Activity[_,_]]: Any = macro activityMeta_impl[A]

  def activityMeta_impl [A <: Activity[_,_] : c.WeakTypeTag] (c: Context) = {
    import c.universe._
    val a = weakTypeOf[A]
    val t = a.members.find(_.name.toString == "apply").get.asMethod.returnType
    c.Expr[Any](q"""new com.gravitydev.traction.amazonswf.SwfActivityMetaBuilder[$t,$a]()""")
  }

  def workflowMeta [W <: Workflow[_]]: Any = macro workflowMeta_impl[W]

  def workflowMeta_impl [W <: Workflow[_] : c.WeakTypeTag] (c: Context) = {
    import c.universe._
    val w = weakTypeOf[W]
    val t = w.members.find(_.name.toString == "flow").get.asMethod.returnType.asInstanceOf[TypeRefApi].args.head
    c.Expr[Any](q"""new com.gravitydev.traction.amazonswf.SwfWorkflowMetaBuilder[$t,$w]()""")
  }

  def carryOn = WaitOnActivities

  def complete [T] (res: T) = CompleteWorkflow(res)

  implicit def toStep1 [T,A<:Activity[_,T]](activity: A with Activity[_,T])(implicit meta: SwfActivityMeta[T, A with Activity[_,T]]): Step1[T] = 
    new Step1(new ActivityStep(activity)(meta))
 
}

