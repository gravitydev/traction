package com.gravitydev.traction
package amazonswf

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.services.simpleworkflow.model._
import akka.actor.{ActorSystem, Props}
import play.api.libs.json.{Format, Json}
import scala.util.control.Exception

class WorkerSystem (swf: AmazonSimpleWorkflowAsyncClient)(implicit system: ActorSystem) extends Logging {
  import system.dispatcher
  import Concurrent._
  
  type MetaWithT[T] = WorkflowMeta[_ <: Workflow[T]]
  
  def run [W <: Workflow[_] : WorkflowMeta : Format](workflow: W) = {
    val meta = implicitly[WorkflowMeta[W]]
    swf.startWorkflowExecutionAsync(
      new StartWorkflowExecutionRequest()
        .withDomain(meta.domain)
        .withWorkflowType(new WorkflowType().withName(meta.name).withVersion(meta.version))
        .withWorkflowId(meta.id(workflow))
        .withInput(Json.stringify(Json.toJson(workflow)))
        .withTaskList(new TaskList().withName(meta.taskList))
    ) recover {case e =>
      logger.error("Error when triggering task", e)
    }
  }
  
  def startWorkflowWorker [W <: Workflow[_]](meta: WorkflowMeta[W]) = {
    implicit val mt = meta
    
    // try to register the workflow
    Exception.catching(classOf[TypeAlreadyExistsException]) opt {
      swf registerWorkflowType {
        new RegisterWorkflowTypeRequest()
          .withDomain(meta.domain)
          .withName(meta.name)
          .withVersion(meta.version)
          .withDefaultExecutionStartToCloseTimeout(meta.defaultExecutionStartToCloseTimeout.toString)
          .withDefaultTaskStartToCloseTimeout(meta.defaultExecutionStartToCloseTimeout.toString)
      }
    } getOrElse logger.info("Workflow " + meta.name + " already registered")
    
    system.actorOf(Props(new WorkflowWorker[W](swf)), name=meta.name+"-workflow")
  }
  
  def startActivityWorker [C, T:Format, A <: Activity[C,T]](meta: ActivityMeta[A with Activity[C,T]], context: C) = {
    implicit val mt = meta
    // try to register the activity
    Exception.catching(classOf[TypeAlreadyExistsException]) opt {
      swf registerActivityType {
        new RegisterActivityTypeRequest()
          .withDomain(meta.domain)
          .withName(meta.name)
          .withVersion(meta.version)
          .withDefaultTaskList(new TaskList().withName(meta.defaultTaskList))
          .withDefaultTaskScheduleToStartTimeout(meta.defaultTaskScheduleToStartTimeout.toString)
          .withDefaultTaskStartToCloseTimeout(meta.defaultTaskStartToCloseTimeout.toString)
      }
    } getOrElse logger.info("Activity " + meta.name + " already registered")
    
    system.actorOf(Props(new ActivityWorker[C,T,A](swf, context)), name=meta.name+"-activity")
  }

}
