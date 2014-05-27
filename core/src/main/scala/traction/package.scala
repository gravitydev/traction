package com.gravitydev.traction

import scala.pickling._, json._

class PickleSerializer [T : SPickler : Unpickler : FastTypeTag] extends Serializer [T] {
  def serialize (obj: T): String = synchronized {
    obj.pickle.value
  }
  def unserialize (data: String): T = synchronized {
    data.unpickle[T]
  }
}

object `package` {
  implicit def pickleSerializer [T : SPickler : Unpickler : FastTypeTag] = synchronized {
    new PickleSerializer[T]
  }
}

