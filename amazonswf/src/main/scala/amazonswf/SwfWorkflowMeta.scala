package com.gravitydev.traction
package amazonswf

case class SwfWorkflowMeta [T:Serializer, W <: Workflow[T]:Serializer] (
  domain: String, 
  name: String, 
  version: String, 
  taskList: String,
  id: W => String,
  defaultExecutionStartToCloseTimeout: Int = 3600,
  defaultTaskStartToCloseTimeout: Int = 3600,
  childPolicy: String = "TERMINATE"
) {
  def parseWorkflow (data: String): W = implicitly[Serializer[W]].unserialize(data)
  def serializeWorkflow (workflow: W): String = implicitly[Serializer[W]].serialize(workflow)

  def serializeResult (result: T): String = implicitly[Serializer[T]].serialize(result)
}

/*
case class SingleActivityWorkflow [C, T : Serializer, A <: Activity[C,T] : SwfActivityMeta : Serializer](activity: A with Activity[C,T]) extends Workflow [T] {
  def flow = activity
}
*/
