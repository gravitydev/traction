package com.gravitydev.traction

import scala.pickling._, json._

class PickleSerializer [T : SPickler : Unpickler : FastTypeTag] extends Serializer [T] {
  def serialize (obj: T): String = obj.pickle.value
  def unserialize (data: String): T = data.unpickle[T]
}

object `package` {
  implicit def pickleSerializer [T : SPickler : Unpickler : FastTypeTag] = new PickleSerializer[T]
}

