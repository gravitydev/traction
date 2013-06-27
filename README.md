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
- All work is queued up, machines can be brought down and brough up safely without losing work.

The central concepts behind Traction are: Workflow and Activity.

Workflow
--------
A Workflow defines an entire body of work to be performed. It defines the sequence of steps to be performed and decides which activities to run. 

Activity
--------
An Activity represents a single unit of work. Most of the heavy lifting is performed by activities. 

```scala
// workflow
case class IndexProjectData (projectId: Long) extends Workflow[Unit] {
  // define the actual process and delegate to activities for the work
  def flow = for {
    values <- CollectChanges(projectId)
    result <- IndexStoryData(values) || IndexIssueData(values) // parallel
  } yield result
}

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

```scala
// machine 1
ws run IndexProjectData(projectId)
```

```scala
// machine 2
ws.startWorkflowWorker[IndexProjectData]()
ws.startActivityWorker[CollectChanges](dbConnection)
ws.startActivityWorker[IndexStoryData](searchServer)
ws.startActivityWorker[IndexIssueData](searchServer)
```

