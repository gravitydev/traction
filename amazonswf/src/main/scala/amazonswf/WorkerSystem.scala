package com.gravitydev.traction
package amazonswf

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.services.simpleworkflow.model._
import akka.actor.{ActorSystem, Props}
import scala.util.control.Exception

class WorkerSystem (swf: AmazonSimpleWorkflowAsyncClient)(implicit system: ActorSystem) extends Logging {
  import system.dispatcher
  import Concurrent._
  
  def run [T, W <: Workflow[T]](workflow: W with Workflow[T])(implicit meta: SwfWorkflowMeta[T,W]) = {
    swf.startWorkflowExecutionAsync(
      new StartWorkflowExecutionRequest()
        .withDomain(meta.domain)
        .withWorkflowType(new WorkflowType().withName(meta.name).withVersion(meta.version))
        .withWorkflowId(meta.id(workflow))
        .withInput(meta.serializeWorkflow(workflow))
        .withTaskList(new TaskList().withName(meta.taskList))
    ) recover {case e =>
      logger.error("Error when triggering task", e)
    }
  }
  
  def startWorkflowWorker [T, W <: Workflow[T]](meta: SwfWorkflowMeta[T,W]) = {
    registerWorkflow(meta) 
    system.actorOf(Props(new WorkflowWorker(swf, meta)), name=meta.name+"-workflow")
  }

  def registerWorkflow [T, W <: Workflow[T]](meta: SwfWorkflowMeta[T,W]) = {
    // try to register the workflow
    Exception.catching(classOf[TypeAlreadyExistsException]) opt {
      swf registerWorkflowType {
        new RegisterWorkflowTypeRequest()
          .withDomain(meta.domain)
          .withName(meta.name)
          .withVersion(meta.version)
          .withDefaultExecutionStartToCloseTimeout(meta.defaultExecutionStartToCloseTimeout.toString)
          .withDefaultTaskStartToCloseTimeout(meta.defaultExecutionStartToCloseTimeout.toString)
          .withDefaultChildPolicy(meta.childPolicy)
      }
    } getOrElse logger.info("Workflow " + meta.name + " already registered")
  }
  
  def startActivityWorker [C, T : Serializer, A <: Activity[C,T] : Serializer](meta: SwfActivityMeta[T,A], context: C) = {
    registerActivity(meta) 
    //system.actorOf(Props(new ActivityWorker[C,T,A](swf, meta, context)), name=meta.name+"-activity")
  }

  def registerActivity[C, T, A <: Activity[C,T]](meta: SwfActivityMeta[T, A with Activity[C,T]]) = {
    // try to register the activity
    Exception.catching(classOf[TypeAlreadyExistsException]) opt {
      swf registerActivityType {
        new RegisterActivityTypeRequest()
          .withDomain(meta.domain)
          .withName(meta.name)
          .withVersion(meta.version)
          .withDefaultTaskList(new TaskList().withName(meta.defaultTaskList))
          .withDefaultTaskScheduleToStartTimeout(meta.defaultTaskScheduleToStartTimeout.toString)
          .withDefaultTaskScheduleToCloseTimeout(meta.defaultTaskScheduleToCloseTimeout.toString)
          .withDefaultTaskHeartbeatTimeout(meta.defaultTaskHeartbeatTimeout.toString)
          .withDefaultTaskStartToCloseTimeout(meta.defaultTaskStartToCloseTimeout.toString)
      }
    } getOrElse logger.info("Activity " + meta.name + " already registered")
  }

}
