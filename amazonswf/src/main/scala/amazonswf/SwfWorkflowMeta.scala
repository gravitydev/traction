package com.gravitydev.traction
package amazonswf

import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.pickling._, json._

// Necessary to allow the usage of named parameters on the activityMeta macro 
class SwfWorkflowMetaBuilder [T:Serializer, W <: Workflow[T]:Serializer] {
  def settings (
    name: String, 
    version: String, 
    taskList: String,
    id: W => String,
    description: String = "",
    defaultExecutionStartToCloseTimeout: Int = 3600,
    defaultTaskStartToCloseTimeout: Int = 60,
    childPolicy: String = "TERMINATE"
  ) = new SwfWorkflowMeta[T,W](
    name, version, taskList, id,
    description,
    defaultExecutionStartToCloseTimeout,
    defaultTaskStartToCloseTimeout,
    childPolicy
  )
}

class SwfWorkflowMeta [T:Serializer, W <: Workflow[T]:Serializer] (
  val name: String, 
  val version: String, 
  val taskList: String,
  val id: W => String,
  val description: String,
  val defaultExecutionStartToCloseTimeout: Int,
  val defaultTaskStartToCloseTimeout: Int,
  val childPolicy: String
) extends StrictLogging {
  def parseWorkflow (data: String): W = implicitly[Serializer[W]].unserialize(data)
  def serializeWorkflow (workflow: W): String = implicitly[Serializer[W]].serialize(workflow)

  def parseResult (data: String): T = {
    logger.info("PARSING: " + data)
    implicitly[Serializer[T]].unserialize(data)
  }
  def serializeResult (result: T): String = implicitly[Serializer[T]].serialize(result)
}

class SwfSingleActivityWorkflow [T: Serializer, A <: Activity[_,T] : Serializer](val activity: A with Activity[_,T])(implicit meta: SwfActivityMeta[T,A]) extends Workflow [T] {
  def flow = activity
}

