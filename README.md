Traction: Distributed Workflow System
=====================================

Traction is a distributed workflow system that runs on Amazon Simple Workflow (SWF). It is written in Scala and it takes advantage of the monadic and applicative characteristic of workflows to create a DSL that is concise and typesafe.

Requirements that Traction is focused on meeting:
- The invocation of a task need not be on the same system that performs the work (distributed).
- Tasks can be made to depend on the result of other tasks.
- Executing a task is always idempotent (always safe to retry).
- Tasks that don't depend on the result of other tasks are run concurrently.
- The whole history of each execution is easily viewable.
- Gracefully handle the introduction of new versions of tasks.
- All work is persisted and queued up, machines can be brought down and brough up safely without losing work.

Architecture
------------
The system consists of workers and a client that can be used to spawn those workers, or invoke work to be performed by them. It uses Akka for all of the concurrency code and it persists all data and distributes work using the facilities of Amazon Simple Workflow.

The central concepts behind Traction are: Workflow, Activity, and their respective Workers.

Workflow
--------
A Workflow defines an entire body of work to be performed. It defines the sequence of steps to be performed and decides which activities to run. 

```scala
// workflow
case class IndexProjectData (projectId: Long) extends Workflow[Unit] {
  // define the actual process and delegate to activities for the work
  def flow: Step[Unit] = for { // type ascription required for now, not sure why
    values <- CollectChanges(projectId)
    result <- IndexStoryData(values) |~| IndexIssueData(values) // parallel
  } yield result
}
```

A workflow can decide to perform (or not perform) certain activities based on the result from previous ones. 

Activity
--------
An Activity represents a single unit of work. Most of the heavy lifting is performed by activities. 

```scala
// activities
case class CollectChanges (projectId: Long) extends Activity[Connection, List[Changes]] {
  def apply (conn: Connection): List[Changes] => ... do the work
}
case class IndexStoryData (values: List[Changes]) extends Activity[SearchServer, Int] {
  def apply (searchServer: SearchServer): Int => ... do the work
}
case class IndexIssueData (values: List[Changes]) extends Activity[SearchServer, Int] {
  def apply (searchServer: SearchServer): Int => ... do the work
}
```

An activity can have a single (can be a tuple) dependency outside of it's parameters which must be provided by the environment running the worker and it is represented by the first type parameter. This dependency is something that is required to perform the activity, but is not required to invoke it. For example: Database connection, server socket, config file, etc.


```scala
// machine 1
ws run IndexProjectData(projectId)
```

```scala
// machine 2
ws.startWorkflowWorker(workflow[IndexProjectData])(instances = 2)
ws.startActivityWorker(activity[CollectChanges], dbConnection)(instances = 4)
ws.startActivityWorker(activity[IndexStoryData], searchServer)(instances = 4)
ws.startActivityWorker(activity[IndexIssueData], searchServer)(instances = 4)
```

