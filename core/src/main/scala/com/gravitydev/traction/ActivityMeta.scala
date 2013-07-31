package com.gravitydev.traction
package amazonswf

import play.api.libs.json.Format

// This should eventually move to the traction-amazonswf sub-project
case class ActivityMeta [A <: Activity[_,_] : Format](
  domain: String, 
  name: String, 
  version: String, 
  description: String = "",
  defaultTaskList: String,
  defaultTaskScheduleToCloseTimeout: Int = 600,
  defaultTaskScheduleToStartTimeout: Int = 600,
  defaultTaskStartToCloseTimeout: Int = 600,
  defaultTaskHeartbeatTimeout: Int = 600,
  id: A => String
) {
  val format = implicitly[Format[A]]
}
