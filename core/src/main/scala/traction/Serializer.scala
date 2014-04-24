package com.gravitydev.traction

trait Serializer[T] {
  def serialize (obj: T): String
  def unserialize (data: String): T
}
object Serializer {
  def apply [T:Serializer] = implicitly[Serializer[T]]

  // convenience
  def serialize [T:Serializer](obj: T) = Serializer[T].serialize(obj)
  def unserialize [T:Serializer](data: String) = Serializer[T].unserialize(data)
}

