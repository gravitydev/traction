package com.gravitydev.traction

import scala.language.implicitConversions
import scala.language.experimental.macros
import scala.reflect.macros.Context

package amazonswf {
  class SwfActivityData [A <: Activity[_,_]](val activity: A, step: Int)(implicit val meta: SwfActivityMeta[_,A]) {
    lazy val id = meta.id(activity)
    lazy val input = step + ":" + meta.serializeActivity(activity)
  }

  class SwfWorkflowData [T, W <: Workflow[T]](val workflow: W)(implicit val meta: SwfWorkflowMeta[T,W])

  /*
  class ActivityScheduleData (
    val meta: amazonswf.SwfActivityMeta[_], 
    val id: String, 
    val input: String,
    val stepNumber: Int
  )
  */
  /*
  object ScheduleActivity {
    def apply [A <: Activity[_,_] : Serializer] (activity: A, meta: amazonswf.SwfActivityMeta[A], stepNumber: Int) = {
      new ScheduleActivity(
        meta = meta,
        id = 
        input = stepNumber + ":" + Serializer[A].serialize(activity)
      )
    }
  }
  */
  case class ScheduleActivities (activities: List[SwfActivityData[_]]) extends Decision

  case object WaitOnActivities extends Decision
  case class CompleteWorkflow (result: String) extends Decision
  case class FailWorkflow (reason: String) extends Decision
}

abstract class Check[T] {
  def run: T
}

package object amazonswf extends System {
  type ActivityMeta[A <: Activity[_,_]] = SwfActivityMeta[_,A]
  type WorkflowMeta[T, W <: Workflow[T]] = SwfWorkflowMeta[T,W]

  type ActivityData = SwfActivityData[_]
  type WorkflowData = SwfWorkflowData[_,_]

  type Complete = CompleteWorkflow
  type CarryOn = WaitOnActivities.type
  type Schedule = ScheduleActivities

 
  /* 
  def activityMeta [A <: Activity[_,_]: Serializer](
    domain: String, 
    name: String, 
    version: String, 
    defaultTaskList: String,
    id: A => String,
    description: String = "",
    defaultTaskScheduleToCloseTimeout: Int = 600,
    defaultTaskScheduleToStartTimeout: Int = 600,
    defaultTaskStartToCloseTimeout: Int = 600,
    defaultTaskHeartbeatTimeout: Int = 600
  )(implicit resultSerializer: Serializer[A#Result]) = new SwfActivityMeta[A](
    domain, 
    name, 
    version, 
    defaultTaskList, 
    id,   
    description, 
    defaultTaskScheduleToCloseTimeout,
    defaultTaskScheduleToStartTimeout,
    defaultTaskStartToCloseTimeout,
    defaultTaskHeartbeatTimeout
  )//(resultSerializer, implicitly[Serializer[A]])
  */

  def activityMeta [A <: Activity[_,_]]: Any = macro activityMeta_impl[A]

  def activityMeta_impl [A <: Activity[_,_] : c.WeakTypeTag] (c: Context) = {
    import c.universe._
    val a = weakTypeOf[A]
    val t = a.members.find(_.name.toString == "apply").get.asMethod.returnType
    c.Expr[Any](q"""new com.gravitydev.traction.amazonswf.SwfActivityMetaBuilder[$t,$a]()""")
  }

  def carryOn = WaitOnActivities

  /*
  def activityData [T, A <: Activity[_,T]](activity: A, step: Int)(implicit meta: ActivityMeta[T,A]): ActivityData = {
    new ActivityScheduleData(
      meta, 
      meta.id(activity), 
      step + ":" + Serializer[A].serialize(activity),
      step
    )
  }
  */

//def activityData[A <: com.gravitydev.traction.Activity[_, _]](activity: A,step: Int)(implicit evidence$2: com.gravitydev.traction.amazonswf.package.ActivityMeta[A],implicit evidence$3: com.gravitydev.traction.Serializer[A]): com.gravitydev.traction.amazonswf.package.ActivityData = ???
 
  def schedule (activities: List[ActivityData]) = ScheduleActivities(activities)

  implicit def toStep [C,T,A<:Activity[C,T]](activity: A with Activity[C,T])(implicit meta: ActivityMeta[A]) = 
    new ActivityStep(activity)

  //implicit def toWrappedActivity [C,T:Serializer,A<:Activity[C,T]:ActivityMeta:Serializer](a: A with Activity[C,T]) = new ActivityWrapper(a)
 
  /* 
  implicit def singleActivityWorkflowM [C,T,A<:Activity[C,T]](implicit meta: ActivityMeta[T,A]) = {
    SwfWorkflowMeta[SingleActivityWorkflow[C,T,A]] (
      domain    = meta.domain,
      name      = meta.name,
      version   = meta.version,
      taskList  = meta.name + ".decisions",
      id        = wf => am.id(wf.activity)
    )
  }
  */
 
  /* 
  private def activityMetaToWorkflowMeta [C,T: Serializer,A<:Activity[C,T]: Serializer] (meta: SwfActivityMeta[A with Activity[C,T]]) = {
    singleActivityWorkflowM[C,T,A](Serializer[T], meta)
  }
  */
 
  /* 
  implicit class ActivityMetaPimp[C,T:Serializer,A<:Activity[C,T]: Serializer](m: SwfActivityMeta[A with Activity[C,T]]) {
    def asWorkflow = activityMetaToWorkflowMeta[C,T,A](m)
  }
  */
 
  /* 
  def activity [A <: Activity[_,_] : ActivityMeta] = implicitly[ActivityMeta[A]]
  def workflow [WF <: Workflow[_] : WorkflowMeta] = implicitly[WorkflowMeta[WF]]
  */
  
}

