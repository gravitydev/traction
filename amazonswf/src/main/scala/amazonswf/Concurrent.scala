package com.gravitydev.traction

import scala.util.control.Exception.allCatch
import scala.util.Try
//import play.api.Play
import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.{Future => JavaFuture}
import java.util.concurrent.{Future => JavaFuture}
import scala.language.{implicitConversions, postfixOps}

object Concurrent {
  implicit def toAkkaFuture [T](javaFuture: JavaFuture[T])(implicit system: ActorSystem) = wrapJavaFutureInAkkaFuture(javaFuture)
  
  private def wrapJavaFutureInAkkaFuture[T](javaFuture: JavaFuture[T], maybeTimeout: Option[FiniteDuration] = None)(implicit system: ActorSystem):
    scala.concurrent.Future[T] = {
    val promise = scala.concurrent.promise[T]
    pollJavaFutureUntilDoneOrCancelled(javaFuture, promise, maybeTimeout.map(t => Deadline.now + t))
    promise.future
  }
  
  private def pollJavaFutureUntilDoneOrCancelled[T](javaFuture: JavaFuture[T], promise: scala.concurrent.Promise[T], maybeTimeout: Option[Deadline] = None)(implicit system: ActorSystem) {
    import system.dispatcher

    if (maybeTimeout.exists(_.isOverdue)) javaFuture.cancel(true);
  
    if (javaFuture.isDone || javaFuture.isCancelled) {
      promise.complete(Try { javaFuture.get })
    } else {
      system.scheduler.scheduleOnce(50 milliseconds) {
        pollJavaFutureUntilDoneOrCancelled(javaFuture, promise, maybeTimeout)
      }
    }
  }
}
