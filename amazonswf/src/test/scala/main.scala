package com.gravitydev.traction
package amazonswf

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import play.api.libs.json.Json

// just making sure types line up
object main {
  implicit val format = Json.format[Test]
  
  case class Test(n: Int, b: Int) extends Activity[Boolean,String] {
    def apply (context: Boolean) = n.toString
  }
  implicit val meta: ActivityMeta[Test] = null
  
  val swf = new AmazonSimpleWorkflowAsyncClient(null:com.amazonaws.auth.AWSCredentials)
  
  val ws = new WorkerSystem(swf)(null)
  
  ws.startWorkflowWorker(activity[Test].asWorkflow)
  ws.startActivityWorker(activity[Test], true)
}
