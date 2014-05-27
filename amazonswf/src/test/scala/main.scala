package com.gravitydev.traction
package amazonswf

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import scala.pickling._, json._
import scalaz.syntax.id._, scalaz.syntax.IdOps

import org.scalatest._

object activities {
  implicit val addA = activityMeta[Add].settings(
    name = "add",
    version = "1",
    defaultTaskList = "",
    id = _ => "" 
  )

  implicit val formatA = activityMeta[Format].settings(
    name = "format",
    version = "1",
    defaultTaskList = "",
    id = _ => "" 
  )

  case class Add (a: Int, b: Int) extends Activity[Unit,Int] {
    def apply (unit: Unit) = a + b
  }

  case class Format (prefix: String, num: Int) extends Activity[Unit,String] {
    def apply (unit: Unit) = prefix + num.toString
  }

  case class Calculate (prefix: String, number: Int) extends Workflow[String] {
    def flow: Step[String] = for {
      sum       <- Add(number, number*2)
      formatted <- Format(prefix, sum)
    } yield formatted
  }
}

class TractionSpec extends FlatSpec with Matchers {
  import activities._

  "A workflow" should "complete given a history of success" in {
    val history = List(
      ActivityComplete(1, 3.right),
      ActivityComplete(2, "$ 3".right)
    ) 

    val res = Calculate("$ ",1).flow.decide(
      history,
      res => CompleteWorkflow(res),
      error => FailWorkflow(error)
    )

    res should be(CompleteWorkflow("$ 3"))
  }

  it should "fail given a failed step" in {

    val history = List(
      ActivityComplete(1, 3.right),
      ActivityComplete[String](2, ("Could not perform formatting": IdOps[String]).left) // where is "left" on string coming from?
    )

    val res = Calculate("$ ",1).flow.decide(
      history,
      res => CompleteWorkflow(res),
      error => FailWorkflow(error)
    )
    
    res should be(FailWorkflow("Could not perform formatting")) 
  }

  it should "schedule the next task" in {
    val history = List(
      ActivityComplete(1, 3.right)
    )

    val res = Calculate("$ ",1).flow.decide(
      history,
      res => CompleteWorkflow(res),
      error => FailWorkflow(error)
    )

    res should be(
      ScheduleActivities(
        List(
          SwfActivityData(Format("$ ", 3), 2)
        )
      )
    ) 
  }

}

// just making sure types line up
object main {
   /*
  implicit val format = Json.format[Test]
  
  case class Test(n: Int, b: Int) extends Activity[Boolean,String] {
    def apply (context: Boolean) = n.toString
  }
  implicit val meta: ActivityMeta[Test] = null
  
  val swf = new AmazonSimpleWorkflowAsyncClient(null:com.amazonaws.auth.AWSCredentials)
  
  val ws = new WorkerSystem(swf)(null)
  
  ws.startWorkflowWorker(activity[Test].asWorkflow)
  ws.startActivityWorker(activity[Test], true)
  */
}
