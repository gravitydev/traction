package com.gravitydev.traction

import scala.language.implicitConversions
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.runtime.universe._
import com.typesafe.scalalogging.slf4j.Logging
import scalaz._, syntax.validation._

package amazonswf {
  private [amazonswf] case class SwfActivityData [A <: Activity[_,_]](val activity: A)(implicit val meta: SwfActivityMeta[_,A]) {
    lazy val id = meta.id(activity)
    lazy val input = meta.serializeActivity(activity)

    override def toString = "SwfActivityData(" + id + ": " + input + ")"
  }

  case class ScheduleActivities (activities: List[SwfActivityData[_]]) extends Decision.Schedule

  case object WaitOnActivities extends Decision.CarryOn
  case class CompleteWorkflow [T] (result: T) extends Decision.Complete[T]
  case class FailWorkflow (reason: String) extends Decision.Fail

  sealed trait ActivityState
  case object ActivityInProcess extends ActivityState
  case class ActivityComplete (result: String \/ String) extends ActivityState


  class ActivityStep [T, A <: Activity[_,T]] (
    val activity: A with Activity[_,T]
  )(implicit meta: SwfActivityMeta[T,A]) extends Step [T] {

    def decide (history: Map[String,ActivityState], onSuccess: T=>Decision, onFailure: String=>Decision): Decision = history.get(meta.id(activity)) map {
      case ActivityComplete(res: \/[String,String]) => res fold (
        error => onFailure(error), 
        result => onSuccess( meta.parseResult(result) )
      ) : Decision
      case ActivityInProcess => carryOn 
    } getOrElse ( ScheduleActivities( List(new SwfActivityData(activity)) ): Decision)
    
  }
}

abstract class Check[T] {
  def run: T
}

package object amazonswf extends System {
  type WorkflowHistory = Map[String,ActivityState]
  type ActivityMeta[A <: Activity[_,_]] = SwfActivityMeta[_,A]
  type WorkflowMeta[T, W <: Workflow[T]] = SwfWorkflowMeta[T,W]

  type Complete[T] = CompleteWorkflow[T]
  type CarryOn = WaitOnActivities.type
  type Schedule = ScheduleActivities 

  def combineSchedules(a: ScheduleActivities, b: ScheduleActivities) = {
    ScheduleActivities(a.activities ++ b.activities)
  }
 
  /** Obtain an ActivityMeta without having to specify the inner types */ 
  def activity [A <: Activity[_,_]]: Any = macro activity_impl[A]

  def activity_impl [A <: Activity[_,_] : c.WeakTypeTag] (c: Context) = {
    import c.universe._
    val a = weakTypeOf[A]
    val t = a.members.find(_.name.toString == "apply").get.asMethod.returnType
    c.Expr[Any](q"""implicitly[SwfActivityMeta[$t,$a]]""")
  }
  
  /** Produce an ActivityMetaBuilder without having to specify the inner types */
  def activityMeta [A <: Activity[_,_]]: Any = macro activityMeta_impl[A]

  def activityMeta_impl [A <: Activity[_,_] : c.WeakTypeTag] (c: Context) = {
    import c.universe._
    val a = weakTypeOf[A]
    val t = a.members.find(_.name.toString == "apply").get.asMethod.returnType
    c.Expr[Any](q"""new com.gravitydev.traction.amazonswf.SwfActivityMetaBuilder[$t,$a]()""")
  }
 
  /** Obtain a WorkflowMeta without having to specify the inner types */ 
  def workflow [W <: Workflow[_]]: Any = macro workflow_impl[W]

  def workflow_impl [W <: Workflow[_] : c.WeakTypeTag] (c: Context) = {
    import c.universe._
    val w = weakTypeOf[W]
    val t = w.members.find(_.name.toString == "flow").get.asMethod.returnType.asInstanceOf[TypeRefApi].args.head
    c.Expr[Any](q"""implicitly[SwfWorkflowMeta[$t,$w]]""")
  }

  /** Produce a WorkflowMetaBuilder without having to specify the inner types */
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

  implicit def toStepList [T,A<:Activity[_,T]](activities: List[A with Activity[_,T]])(implicit meta: SwfActivityMeta[T, A with Activity[_,T]]): List[Step1[T]] = 
    activities.map(a => toStep1(a)(meta))
 
}

