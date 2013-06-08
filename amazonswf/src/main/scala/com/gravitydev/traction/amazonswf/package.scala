package com.gravitydev.traction

import play.api.libs.json._
import scala.language.implicitConversions

package object amazonswf {
  
  implicit def toInvocation [C,T,A<:Activity[C,T]:ActivityMeta](activity: A with Activity[C,T]) = new ActivityInvocation(1, activity)

  implicit def singleActivityWorkflowF [C,T,A<:Activity[C,T]:Format:ActivityMeta] = new Format [SingleActivityWorkflow[C,T,A]] {
    def reads (json: JsValue) = Json.fromJson[A](json \ "activity") map {a => SingleActivityWorkflow(a)}
    def writes (wf: SingleActivityWorkflow[C,T,A]) = Json.obj("activity" -> Json.toJson(wf.activity))
  }
  
  implicit def toWrappedActivity [C,T,A<:Activity[C,T]:Format:ActivityMeta](a: A with Activity[C,T]) = new ActivityWrapper(a)
  
  implicit def singleActivityWorkflowM [C,T,A<:Activity[C,T]:ActivityMeta:Format] = {
    val am = implicitly[ActivityMeta[A]]
    WorkflowMeta[SingleActivityWorkflow[C,T,A]] (
      domain    = am.domain,
      name      = am.name,
      version   = am.version,
      taskList  = am.name + ".decisions",
      id        = wf => am.id(wf.activity)
    )
  }
  
  private def activityMetaToWorkflowMeta [C,T,A<:Activity[C,T]:Format] (meta: ActivityMeta[A with Activity[C,T]]) = {
    singleActivityWorkflowM[C,T,A](meta, implicitly[Format[A]])
  }
  
  implicit class ActivityMetaPimp[C,T,A<:Activity[C,T]:Format](m: ActivityMeta[A with Activity[C,T]]) {
    def asWorkflow = activityMetaToWorkflowMeta[C,T,A](m)
  }
  
  // here for activities that produce Unit
  implicit val unitFormat = new Format [Unit] {
    def reads (json: JsValue) = JsSuccess(())
    def writes (o: Unit) = JsNull
  }

  def activity [A <: Activity[_,_] : ActivityMeta] = implicitly[ActivityMeta[A]]
  def workflow [WF <: Workflow[_] : WorkflowMeta] = implicitly[WorkflowMeta[WF]]
  
}
