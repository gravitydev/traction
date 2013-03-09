package workflow

import com.amazonaws.services.simpleworkflow, 
  simpleworkflow.model._

/*
object WorkflowUtil {
  implicit def toSwf (decision: Decision): Seq[simpleworkflow.model.Decision] = decision match {
    case CompleteWorkflowExecution(result) => 
      Seq(new simpleworkflow.model.Decision()
        .withDecisionType(DecisionType.CompleteWorkflowExecution)
        .withCompleteWorkflowExecutionDecisionAttributes(
          new CompleteWorkflowExecutionDecisionAttributes()
            .withResult(result)
        ))
    case ScheduleActivityTask(t, taskList) => 
      Seq(new simpleworkflow.model.Decision()
        .withDecisionType(DecisionType.ScheduleActivityTask)
        .withScheduleActivityTaskDecisionAttributes(
           new ScheduleActivityTaskDecisionAttributes()
             .withActivityId("time-"+new java.util.Date().getTime/1000)
             .withActivityType(
               new ActivityType().withName(t.activityType).withVersion(t.activityVersion)
             )
             .withTaskList(new TaskList().withName(taskList))
             .withInput(t.input)
         ))
    case ScheduleMultipleActivityTasks(tasks) => tasks flatMap (toSwf _)
  }
}
*/