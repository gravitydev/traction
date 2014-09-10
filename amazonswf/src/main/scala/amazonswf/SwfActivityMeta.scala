package com.gravitydev.traction
package amazonswf

// Necessary to allow the usage of named parameters on the activityMeta macro 
class SwfActivityMetaBuilder [T:Serializer,A <: Activity[_,T]:Serializer] {
  def settings (
    name: String, 
    version: String, 
    defaultTaskList: String,
    id: A => String,
    description: String = "",
    defaultTaskScheduleToCloseTimeout: Int = 600,
    defaultTaskScheduleToStartTimeout: Int = 600,
    defaultTaskStartToCloseTimeout: Int = 600,
    defaultTaskHeartbeatTimeout: Int = 600
  ) = new SwfActivityMeta[T,A](
    name, version, defaultTaskList, id, description, 
    defaultTaskScheduleToCloseTimeout, 
    defaultTaskScheduleToStartTimeout,
    defaultTaskStartToCloseTimeout,
    defaultTaskHeartbeatTimeout 
  )
}

class SwfActivityMeta [T, A <: Activity[_,T]] (
  val name: String, 
  val version: String, 
  val defaultTaskList: String,
  val id: A => String,
  val description: String,
  val defaultTaskScheduleToCloseTimeout: Int,
  val defaultTaskScheduleToStartTimeout: Int,
  val defaultTaskStartToCloseTimeout: Int,
  val defaultTaskHeartbeatTimeout: Int
)(implicit resultS: Serializer[T], activityS: Serializer[A]) {
  def parseActivity (data: String): A = activityS.unserialize(data)
  def serializeActivity(activity: A): String = activityS.serialize(activity)

  def parseResult (data: String): T = resultS.unserialize(data)
  def serializeResult(result: T): String = resultS.serialize(result/*.asInstanceOf[T]*/)

  def toWorkflowMeta (implicit s2: Serializer[T], s1: Serializer[SwfSingleActivityWorkflow[T,A]]): SwfWorkflowMeta[T,SwfSingleActivityWorkflow[T,A]] = {
    //val s1: Serializer[SwfSingleActivityWorkflow[T,A]] = ??? //implicitly[Serializer[SwfSingleActivityWorkflow[T,A]]]
    //val s2 = implicitly[Serializer[T]]

    (new SwfWorkflowMetaBuilder[T, SwfSingleActivityWorkflow[T,A]]()(s2,s1))
      .settings(
        name      = name,
        version   = version,
        taskList  = name + ".workflow",
        id        = wf => id(wf.activity)
      )
  }
}

