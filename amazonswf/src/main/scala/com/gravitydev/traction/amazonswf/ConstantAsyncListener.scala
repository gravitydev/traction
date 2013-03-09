package com.gravitydev.traction
package amazonswf

import akka.actor._
import scala.concurrent.Future
import akka.agent.Agent
import scala.concurrent.duration._

trait ConstantAsyncListener extends Actor with ActorLogging {
  implicit val system = context.system
  import system.dispatcher
  
  val retriesSoFar = Agent(0)
  
  def listen: Future[Unit]
  
  // auto start
  override def preStart() = {
    self ! ()
  }
  
  def receive = {case _ =>
    listen map {_ =>
      // keep going
      self ! ()
    } recover {case e => 
      // log
      log.error(e, "Error while listening. Retries so far: " + retriesSoFar())
      
      // update retries
      retriesSoFar send (_+1)
      
      // retry
      system.scheduler.scheduleOnce(math.min(retriesSoFar(), 40).second, self, ())
    }
  }
}
