package com.gravitydev.traction
package amazonswf

import play.api.libs.json._

class ActivityWrapper [C, T, A <: Activity[C,T] : Format : ActivityMeta] (a: A with Activity[C,T]) {
  def asWorkflow = SingleActivityWorkflow(a)
}
