package com.gravitydev.traction
package amazonswf

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.services.simpleworkflow.model._
import akka.actor.{ActorSystem, Props}
import akka.routing.BroadcastRouter
import scala.util.control.Exception
import scala.language.experimental.macros
import com.typesafe.scalalogging.slf4j.StrictLogging
import com.gravitydev.awsutil.awsToScala

class WorkerSystem (domain: String, swf: AmazonSimpleWorkflowAsyncClient)(implicit system: ActorSystem) extends StrictLogging {
  import system.dispatcher
  
  def run [T, W <: Workflow[T]](workflow: W with Workflow[T])(implicit meta: SwfWorkflowMeta[T,W]) = {
    logger.info("Input: " + meta.serializeWorkflow(workflow))
    awsToScala(swf.startWorkflowExecutionAsync)(
      new StartWorkflowExecutionRequest()
        .withDomain(domain)
        .withWorkflowType(new WorkflowType().withName(meta.name).withVersion(meta.version))
        .withWorkflowId(meta.id(workflow))
        .withInput(meta.serializeWorkflow(workflow))
        .withTaskList(new TaskList().withName(meta.taskList))
    ) recover {case e =>
      logger.error("Error when triggering task", e)
    }
  }
  
  def startWorkflowWorker [T, W <: Workflow[T]](meta: SwfWorkflowMeta[T,W])(instances: Int = 1) = {
    registerWorkflow(meta) 
    system.actorOf(
      Props(new WorkflowWorker(domain, swf, meta)).withRouter(BroadcastRouter(instances)),
      name=meta.name+"-workflow"
    )
  }

  // TODO: use macros to allow starting the activity worker with just the A type param
  def startActivityWorker [C, T : Serializer, A <: Activity[C,T] : Serializer](meta: SwfActivityMeta[T,A], context: C)(instances: Int = 1) = {
    registerActivity(meta) 
    system.actorOf(
      Props(new ActivityWorker[C,T,A](domain, swf, meta, context)).withRouter(BroadcastRouter(instances)), 
      name=meta.name+"-activity"
    )
  }

  def registerWorkflow [W <: Workflow[_]](implicit meta: SwfWorkflowMeta[_,W]) = {
    // try to register the workflow
    Exception.catching(classOf[TypeAlreadyExistsException]) opt {
      swf registerWorkflowType {
        new RegisterWorkflowTypeRequest()
          .withDomain(domain)
          .withName(meta.name)
          .withVersion(meta.version)
          .withDefaultExecutionStartToCloseTimeout(meta.defaultExecutionStartToCloseTimeout.toString)
          .withDefaultTaskStartToCloseTimeout(meta.defaultExecutionStartToCloseTimeout.toString)
          .withDefaultChildPolicy(meta.childPolicy)
      }
    } getOrElse logger.warn("Workflow " + meta.name + " already registered")
  }
  
  def registerActivity[A <: Activity[_,_]](implicit meta: SwfActivityMeta[_, A with Activity[_,_]]) = {
    // try to register the activity
    Exception.catching(classOf[TypeAlreadyExistsException]) opt {
      swf registerActivityType {
        new RegisterActivityTypeRequest()
          .withDomain(domain)
          .withName(meta.name)
          .withVersion(meta.version)
          .withDefaultTaskList(new TaskList().withName(meta.defaultTaskList))
          .withDefaultTaskScheduleToStartTimeout(meta.defaultTaskScheduleToStartTimeout.toString)
          .withDefaultTaskScheduleToCloseTimeout(meta.defaultTaskScheduleToCloseTimeout.toString)
          .withDefaultTaskHeartbeatTimeout(meta.defaultTaskHeartbeatTimeout.toString)
          .withDefaultTaskStartToCloseTimeout(meta.defaultTaskStartToCloseTimeout.toString)
      }
    } getOrElse logger.warn("Activity " + meta.name + " already registered")
  }

}
