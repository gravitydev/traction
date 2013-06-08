package com.gravitydev.traction
package amazonswf

import play.api.libs.json.Format

case class ActivityMeta [A <: Activity[_,_] : Format](
  domain: String, 
  name: String, 
  version: String, 
  description: String = "",
  defaultTaskList: String,
  defaultTaskScheduleToCloseTimeout: Int = 0,
  defaultTaskScheduleToStartTimeout: Int = 0,
  defaultTaskStartToCloseTimeout: Int = 0,
  defaultTaskHeartbeatTimeout: Int = 0,
  id: A => String
) {
  val format = implicitly[Format[A]]
}
