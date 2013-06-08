package com.gravitydev.traction
package amazonswf

import akka.actor.{Actor, ActorLogging}
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.services.simpleworkflow.model._
import Concurrent._
import play.api.libs.json.{Json, Format}
import scala.language.postfixOps

class ActivityWorker [C, T : Format, A <: Activity[C,T] : ActivityMeta] (swf: AmazonSimpleWorkflowAsyncClient, context: C) extends ConstantAsyncListener {
  import system.dispatcher
  val meta = implicitly[ActivityMeta[A]]
  
  def listen = {
    log.info("Listening for tasks on: " + meta.defaultTaskList)
    
    swf pollForActivityTaskAsync {
      new PollForActivityTaskRequest()
        .withDomain(meta.domain)
        .withTaskList(new TaskList().withName(meta.defaultTaskList))
    } map {task =>
      // if there is a task
      Option(task.getTaskToken) filter (_!="") foreach {token =>
        val activity = meta.format.reads(Json.parse((task.getInput span (_!=':') _2) drop 1)) get;
        log.info("Activity task: "+activity)
        val result = activity(context)
        
        swf respondActivityTaskCompletedAsync {
          new RespondActivityTaskCompletedRequest()
            .withTaskToken(token)
            .withResult(Json.stringify(implicitly[Format[T]].writes(result.asInstanceOf[T])))
        }
      }
      ()
    } recover {
      case e => log.error(e, "Error polling")
    }
  }
}
