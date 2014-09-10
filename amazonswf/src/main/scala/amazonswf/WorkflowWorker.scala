package com.gravitydev.traction
package amazonswf

import akka.actor.{Actor, ActorLogging, Props, FSM}
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient
import com.amazonaws.services.simpleworkflow.model.{Decision => SwfDecision, _}
import scala.collection.JavaConversions._
import scala.language.postfixOps
import scalaz._, std.either._, syntax.id._
import com.gravitydev.awsutil.awsToScala

class WorkflowWorker [T, W <: Workflow[T]](domain: String, swf: AmazonSimpleWorkflowAsyncClient, meta: SwfWorkflowMeta[T,W]) extends ConstantAsyncListener {
  import system.dispatcher
  
  def listen = {
    log.info("Listening for tasks on: " + meta.taskList)

    awsToScala(swf.pollForDecisionTaskAsync)(
      new PollForDecisionTaskRequest()
        .withDomain(domain)
        .withTaskList(new TaskList().withName(meta.taskList))
    ) map {task =>
      log.info("Received task: " + task.getTaskToken)
      // if there is a task
      Option(task.getTaskToken) filter (_!="") foreach {token =>
        val events = task.getEvents.toList
        
        // find workflow
        val workflow = events collect {case SwfEvents.WorkflowExecutionStarted(attr) => meta.parseWorkflow(attr.getInput)} head
        
        val hist = history(events)
        log.info("History: " + hist)
        
        val st = state(hist)
        
        log.info("State: " + st)
        val decision = workflow.flow.decide(
          st, 
          result => CompleteWorkflow(result),
          error => FailWorkflow(error)
        )
        
        log.info("Decision: " + decision)
       
        awsToScala(swf.respondDecisionTaskCompletedAsync)(
          new RespondDecisionTaskCompletedRequest()
            .withDecisions (
              decision match {
                case ScheduleActivities(activities) => activities map {a =>
                  new SwfDecision()
                    .withDecisionType(DecisionType.ScheduleActivityTask)
                    .withScheduleActivityTaskDecisionAttributes(
                      new ScheduleActivityTaskDecisionAttributes()
                        .withActivityType(
                          new ActivityType()
                            .withName(a.meta.name)
                            .withVersion(a.meta.version)  
                        )
                        .withActivityId(a.id)
                        .withTaskList(
                          new TaskList().withName(a.meta.defaultTaskList)
                        )
                        .withInput(a.input)
                    )
                }
                case WaitOnActivities => Nil
                case CompleteWorkflow(res) => List(
                  new SwfDecision()
                    .withDecisionType(DecisionType.CompleteWorkflowExecution)
                    .withCompleteWorkflowExecutionDecisionAttributes(
                      new CompleteWorkflowExecutionDecisionAttributes()
                        .withResult(meta.serializeResult(res.asInstanceOf[T]))
                    )
                )
              }
            )
            .withTaskToken(token)
        ) recover {
          case e => log.error(e, "Error responding to decision task")
        }
      }
      ()
    } recover {
      case e => log.error(e, "Error when polling")
    }
  }
  
  def history (events: List[HistoryEvent]): List[ActivityEvent] = {
    def getActivityId (eventId: Long): String = (events.find(_.getEventId == eventId).get.getActivityTaskScheduledEventAttributes.getActivityId)
      
    events collect {
      case SwfEvents.ActivityTaskScheduled(attr)  => ActivityScheduled(attr.getActivityId)
      case SwfEvents.ActivityTaskStarted(attr)    => ActivityStarted(getActivityId(attr.getScheduledEventId))
      case SwfEvents.ActivityTaskCompleted(attr)  => {
        log.info("ATTR: " + attr)
        ActivitySucceeded(getActivityId(attr.getScheduledEventId), attr.getResult)
      }
      case SwfEvents.ActivityTaskFailed(attr)     => ActivityFailed(getActivityId(attr.getScheduledEventId), attr.getReason+": "+attr.getDetails)
    }
  }

  def state (hist: List[ActivityEvent]): Map[String,ActivityState] = (hist collect {
    case scheduled @ ActivityScheduled(activityId) => {
      // find the last event relating this activity
      hist.reverse.find(ev => ev.activityId == activityId && scheduled != ev) flatMap {
        case ActivitySucceeded(id, res) => {
          log.info("Success: " + id + ": " + res)
          Some(id -> ActivityComplete(res.right[String]))
        }
        case ActivityFailed(id, reason) => Some(id -> ActivityComplete(reason.left))
        case ActivityStarted(id) => Some(id -> ActivityInProcess)
        case x => sys.error("Unhandled event: " + x)
      } getOrElse activityId -> ActivityInProcess
    }
  }).toMap

}

