package com.gravitydev.traction
package amazonswf

import play.api.libs.json.Format

case class WorkflowMeta [W <: Workflow[_] : Format] (
  domain: String, 
  name: String, 
  version: String, 
  taskList: String,
  id: W => String,
  defaultExecutionStartToCloseTimeout: Int = 3600,
  defaultTaskStartToCloseTimeout: Int = 3600,
  childPolicy: String = "TERMINATE"
) {
  val format = implicitly[Format[W]]
}

case class SingleActivityWorkflow [C, T, A <: Activity[C,T] : ActivityMeta : Format](activity: A with Activity[C,T]) extends Workflow [T] {
  def flow = activity
}

