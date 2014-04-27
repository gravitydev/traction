package com.gravitydev.traction
package amazonswf

private [amazonswf] class SwfActivityMetaBuilder [T,A <: Activity[_,T]:Serializer] {
  def settings (
    domain: String,
    name: String, 
    version: String, 
    defaultTaskList: String,
    id: A => String
  ) = new SwfActivityMeta[T,A](domain, name, version, defaultTaskList, id)
}

class SwfActivityMeta [T, A <: Activity[_,T]] (
  val domain: String, 
  val name: String, 
  val version: String, 
  val defaultTaskList: String,
  val id: A => String,
  val description: String = "",
  val defaultTaskScheduleToCloseTimeout: Int = 600,
  val defaultTaskScheduleToStartTimeout: Int = 600,
  val defaultTaskStartToCloseTimeout: Int = 600,
  val defaultTaskHeartbeatTimeout: Int = 600
)(implicit /*resultS: Serializer[A#Result], */activityS: Serializer[A]) {
  def parseActivity (data: String): A = activityS.unserialize(data)
  def serializeActivity(activity: A): String = activityS.serialize(activity)

  //def parseResult (data: String): A#Result = resultS.unserialize(data)
  //def serializeResult(result: A#Result): String = resultS.serialize(result.asInstanceOf[A#Result])
}

/*
object SwfActivityMeta {
  def apply [A <: Activity[_,_]](
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
  ) = new SwfActivityMeta(
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
  )
}
*/
