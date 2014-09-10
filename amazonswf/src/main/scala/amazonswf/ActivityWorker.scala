package com.gravitydev.traction
package amazonswf

import com.gravitydev.awsutil._
import akka.actor.{Actor, ActorLogging}
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.services.simpleworkflow.model._
import com.gravitydev.awsutil.awsToScala
import scala.language.postfixOps

class ActivityWorker [C, T, A <: Activity[C,T]] (
  domain: String, swf: AmazonSimpleWorkflowAsyncClient, meta: SwfActivityMeta[T,A], context: C
) extends ConstantAsyncListener {
  import system.dispatcher

  def listen = {
    log.info("Listening for tasks on: " + meta.defaultTaskList)

    com.gravitydev.awsutil.awsToScala(swf.pollForActivityTaskAsync)(
      new PollForActivityTaskRequest()
        .withDomain(domain)
        .withTaskList(new TaskList().withName(meta.defaultTaskList))
    ) map {task =>
      // if there is a task
      Option(task.getTaskToken) filter (_!="") foreach {token =>
        //val activity = meta.format.reads(Json.parse((task.getInput span (_!=':') _2) drop 1)) get;
        val activity = meta.parseActivity(task.getInput)

        log.info("Activity task: "+activity)
        
        try {
          val result: T = activity(context)
          
          swf respondActivityTaskCompletedAsync {
            new RespondActivityTaskCompletedRequest()
              .withTaskToken(token)
              .withResult(meta.serializeResult(result))
          }
        } catch {
          case e: Throwable => swf.respondActivityTaskFailedAsync {
            new RespondActivityTaskFailedRequest()
              .withTaskToken(token)
              .withReason("Exception thrown")
              .withDetails(e.getMessage() + "\n" + e.getStackTraceString)
          }
        }
      }
      ()
    } recover {
      case e => log.error(e, "Error polling")
    }
  }
}

