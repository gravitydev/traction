package com.gravitydev.traction
package amazonswf

import play.api.libs.json.Format

case class ActivityMeta [A <: Activity[_,_] : Format](
  domain: String, 
  name: String, 
  version: String, 
  description: String = "",
  taskList: String,
  taskScheduleToCloseTimeout: Int = 0,
  taskScheduleToStartTimeout: Int = 0,
  taskStartToCloseTimeout: Int = 0,
  taskHeartbeatTimeout: Int = 0,
  id: A => String
) {
  val format = implicitly[Format[A]]
}
