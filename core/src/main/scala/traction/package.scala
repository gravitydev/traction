package com.gravitydev.traction

import upickle._

class PickleSerializer [T : Reader : Writer] extends Serializer [T] {
  def serialize (obj: T): String = write(obj)
  def unserialize (data: String): T = read[T](data)
}

object `package` {
  implicit def pickleSerializer [T : Reader : Writer] = new PickleSerializer[T]
}

