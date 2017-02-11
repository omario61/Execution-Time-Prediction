/*
Copyright (c) 2016, Rice University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
2.  Redistributions in binary form must reproduce the above
     copyright notice, this list of conditions and the following
     disclaimer in the documentation and/or other materials provided
     with the distribution.
3.  Neither the name of Rice University
     nor the names of its contributors may be used to endorse or
     promote products derived from this software without specific
     prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.apache.spark.rdd.cl

import java.util.LinkedList
import java.lang.reflect.Field

import scala.reflect.ClassTag

import com.amd.aparapi.internal.model.ClassModel
import com.amd.aparapi.internal.model.ClassModel
import com.amd.aparapi.internal.model.Entrypoint
import com.amd.aparapi.internal.model.HardCodedClassModels.ShouldNotCallMatcher
import com.amd.aparapi.internal.writer.ScalaArrayParameter

class JVMAsyncOutputStream[U: ClassTag, M: ClassTag](val singleInstance : Boolean)
    extends AsyncOutputStream[U, M] {

  val buffered : LinkedList[U] = new LinkedList[U]
  val metadataBuffered : LinkedList[Option[M]] = new LinkedList[Option[M]]

  override def spawn(l : () => U, metadata : Option[M]) {
    val value = l()
    assert(value != null)

    buffered.add(value)
    metadataBuffered.add(metadata)

    if (singleInstance) {
      throw new SuspendException
    }
  }

  def pop() : Option[Tuple2[U, Option[M]]] = {
    var result : Option[U] = None

    if (buffered.isEmpty) {
      None
    } else {
      Some((buffered.poll, metadataBuffered.poll))
    }
  }

  def isEmpty() : Boolean = buffered.isEmpty
}