import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.auth.BasicAWSCredentials

import akka.actor.ActorSystem
import com.gravitydev.traction._, amazonswf._
import scalaz._, syntax.id._
import scala.pickling._, json._

object Sample extends App {
  implicit val system = ActorSystem("traction-sample")
  implicit val ec = system.dispatcher
  
  implicit val helloWorldM = activityMeta[HelloWorld](
    domain="dev.gravitydev.com",
   version="1.0",
    name="hello-world-4",
    defaultTaskList="hello-world",
    id = _ => "2"
  )
  
  implicit val doubleNumberA = activityMeta[DoubleNumber](
    domain="dev.gravitydev.com",
    version="1.0",
    name="double-number",
    defaultTaskList="double-number",
    id = x => x.number.toString
  )
  
  implicit val printNumberA = activityMeta[PrintNumber](
    domain="dev.gravitydev.com",
    version="1.0",
    name="print-number",
    defaultTaskList="print-number",
    id = _.number.toString
  )
  
  implicit val showNumberW = WorkflowMeta[ShowNumber](
    domain="dev.gravitydev.com",
    version="1.0",
    name="show-number",
    taskList="show-number",
    id = _.number.toString
  )
  
	case class ShowNumber (number: Int) extends Workflow[String] {
	  def flow = for {
	    number   <- DoubleNumber(number)
	    n        <- PrintNumber(number+3) && DoubleNumber(number)
	  } yield n.toString
	}
  
  /*
	case class Test (number: Int) extends Workflow[String] {
	  def flow = for {
	    n <- DoubleNumber(number) && DoubleNumber(number)
	    x <- DoubleNumber(n._1)
	  } yield n.toString
	}
	*/

  val creds = new BasicAWSCredentials("xxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxx")
  val swf = new AmazonSimpleWorkflowAsyncClient(creds)

  val ws = new WorkerSystem(swf)

  /*
  ws.registerActivity(implicitly[ActivityMeta[HelloWorld]])
  ws.registerWorkflow(implicitly[ActivityMeta[HelloWorld]].asWorkflow)
  
  ws run HelloWorld(name="Alvaro").asWorkflow

  ws.startWorkflowWorker(implicitly[ActivityMeta[HelloWorld]].asWorkflow)
  ws.startActivityWorker(implicitly[ActivityMeta[HelloWorld]], context = ()
  */
  
  /*
  ws.registerActivity(doubleNumberA)
  ws.registerWorkflow(showNumberW)
  */
  
  /*
  ws.run(ShowNumber(25)) map {x =>
    println(x)
  }
  
  ws.startWorkflowWorker(showNumberW)
  ws.startActivityWorker(doubleNumberA, context = ())
  */

  val four = Serializer.serialize(4)
  val fourS = Serializer.serialize("4")
  val eight = Serializer.serialize(8)

  val history = List[ActivityState](
    ActivityComplete(1, four.right),
    ActivityComplete(2, fourS.right),
    ActivityComplete(3, eight.right)
  )
  
  val res = ShowNumber(1).flow.decide(history, res => CompleteWorkflow(res), error => FailWorkflow(error))
  
  println(res)
}

case class HelloWorld (name: String) extends Activity [Unit,Unit] {
  def apply (x: Unit) = {
    println("Hello World")
  }
}

case class DoubleNumber (number: Int) extends Activity[Unit,Int] {
  def apply (ctx: Unit) = number * 2
}
case class PrintNumber (number: Int) extends Activity[Unit,String] {
  def apply (ctx: Unit) = number.toString
}

/*
case class PrintPair (pair: (Int,Int)) extends Activity[Unit,String] {
  def apply (ctx: Unit) = pair.toString
}
*/
