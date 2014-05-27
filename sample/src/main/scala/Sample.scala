import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.auth.BasicAWSCredentials

import akka.actor.ActorSystem
import com.gravitydev.traction._, amazonswf._
import scalaz._, syntax.id._
import scala.pickling._, json._

object Sample extends App {
  implicit val system = ActorSystem("traction-sample")
  implicit val ec = system.dispatcher

  implicit val doubleNumberA = activityMeta[DoubleNumber].settings(
    name="double-number",
    version="1.0",
    defaultTaskList="double-number",
    id = "double-number-" + _.number.toString
  )
  
  implicit val printNumberA = activityMeta[PrintNumber].settings(
    name="print-number",
    version="1.0",
    defaultTaskList="print-number",
    id = "print-number-" + _.number.toString
  )
  
  implicit val showNumberW = workflowMeta[ShowNumber].settings(
    name="show-number",
    version="1.1",
    taskList="show-number",
    id = "show-number-" + _.number.toString
  )
  
	case class ShowNumber (number: Int) extends Workflow[(String,String,String)] {
	  def flow: Step[(String,String,String)] = for {  
	    number   <- DoubleNumber(number)
      _        <- Step.list( List(PrintNumber(number+30), PrintNumber(1), PrintNumber(2)) )
	    n        <- PrintNumber(number) |~| PrintNumber(number+1) |~| PrintNumber(number+2)
	  } yield n
	}
  
  val creds = new BasicAWSCredentials("xxxxxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxxxxxx")
  val swf = new AmazonSimpleWorkflowAsyncClient(creds)

  val ws = new WorkerSystem("dev.gravitydev.com", swf)

  ws.registerActivity[PrintNumber] 
  ws.registerActivity[DoubleNumber]
  ws.registerWorkflow[ShowNumber]
 
  for (i <- 1 to 10) { 
    ws.run(ShowNumber(i)) map {x =>
      println(x)
    }
  }
  
  ws.startWorkflowWorker(workflow[ShowNumber])(instances = 1)
  ws.startActivityWorker(activity[DoubleNumber], context = ())(instances = 5)
  ws.startActivityWorker(activity[PrintNumber], context = ())(instances = 5)
}

/*
case class HelloWorld (name: String) extends Activity [Unit,Unit] {
  def apply (x: Unit) = {
    println("Hello World")
  }
}
*/

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
