package com.gravitydev.traction
package amazonswf

import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.services.simpleworkflow.model._
import Concurrent._
import scala.collection.JavaConversions._
import play.api.libs.json.Json

class WorkflowWorker [W <: Workflow[_] : WorkflowMeta](swf: AmazonSimpleWorkflowAsyncClient) extends ConstantAsyncListener {
  val meta = implicitly[WorkflowMeta[W]]
  import system.dispatcher
  
  def listen = {
    log.info("Listening for tasks on: " + meta.taskList)
    
    swf pollForDecisionTaskAsync {
      new PollForDecisionTaskRequest()
        .withDomain(meta.domain)
        .withTaskList(new TaskList().withName(meta.taskList))
    } map {task => 
      // if there is a task
      Option(task.getTaskToken) filter (_!="") foreach {token =>
        val events = task.getEvents.toList
        
        // find workflow
        val workflow = events collect {case SwfEvents.WorkflowExecutionStarted(attr) => meta.format.reads(Json.parse(attr.getInput)).get} head
        
        val hist = history(events)
        log.info("History: " + hist)
        
        val st = state(hist)
        
        log.info("State: " + st)
        val decision = workflow.flow.decide(st)
        
        log.info("Decision: " + decision)
        
        swf respondDecisionTaskCompletedAsync {
          new RespondDecisionTaskCompletedRequest()
            .withDecisions (decision match {
              case ScheduleActivities(activities) => activities map {a =>
                new Decision()
                  .withDecisionType(DecisionType.ScheduleActivityTask)
                  .withScheduleActivityTaskDecisionAttributes(
                    new ScheduleActivityTaskDecisionAttributes()
                      .withActivityType(
                        new ActivityType().withName(a.meta.name).withVersion(a.meta.version)  
                      )
                      .withActivityId(a.id)
                      .withTaskList(
                        new TaskList().withName(a.meta.taskList)
                      )
                      .withInput(a.input)
                  )
              }
              case WaitOnActivities => Nil
              case CompleteWorkflow(res) => List(
                new Decision()
                  .withDecisionType(DecisionType.CompleteWorkflowExecution)
                  .withCompleteWorkflowExecutionDecisionAttributes(
                    new CompleteWorkflowExecutionDecisionAttributes()
                      .withResult(res)
                  )
              )
            })
            .withTaskToken(token)
        } recover {
          case e => log.error(e, "Error responding to decision task")
        }
      }
      ()
    } recover {
      case e => log.error(e, "Error when polling")
    }
  }
  
  def history (events: List[HistoryEvent]) = {
    def getStepNumber (eventId: Long) = 
      (events.find(_.getEventId == eventId).get.getActivityTaskScheduledEventAttributes.getInput span (_!=':') _1).toInt
      
    events collect {
      case SwfEvents.ActivityTaskStarted(attr)    => ActivityStarted(getStepNumber(attr.getScheduledEventId))
      case SwfEvents.ActivityTaskCompleted(attr)  => ActivitySucceeded(getStepNumber(attr.getScheduledEventId), attr.getResult)
      case SwfEvents.ActivityTaskFailed(attr)     => ActivityFailed(getStepNumber(attr.getScheduledEventId), attr.getReason+": "+attr.getDetails)
    }
  }
  
  def state (hist: List[ActivityEvent]): List[ActivityState] = hist collect {
    case started @ ActivityStarted(stepNumber) => hist.find(ev => ev.stepNumber == stepNumber && started != ev) flatMap {
      case ActivitySucceeded(num, res) => Some(ActivityComplete(num, Right(res)))
      case ActivityFailed(num, reason) => Some(ActivityComplete(num, Left(reason)))
      case x => sys.error("Unhandled event: " + x)
    } getOrElse ActivityInProcess(stepNumber)
  }

}
