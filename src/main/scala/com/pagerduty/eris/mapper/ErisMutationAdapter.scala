/*
 * Copyright (c) 2015, PagerDuty
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.pagerduty.eris.mapper

import java.nio.ByteBuffer
import com.netflix.astyanax.{Serializer, ColumnListMutation}
import com.pagerduty.mapper.{EntityMapperException, MutationAdapter}
import scala.util.control.NonFatal


/**
 * Eris implementation of [[MutationAdapter]].
 */
private[mapper] class ErisMutationAdapter(mutation: ColumnListMutation[String])
  extends MutationAdapter
{
  private def serialize(targetId: Any, colName: String, serializer: Any, value: Any): ByteBuffer = {
    try {
      val usableSerializer = serializer.asInstanceOf[Serializer[Any]]
      usableSerializer.toByteBuffer(value)
    }
    catch { // Adding columnName to exception message to simplify debugging.
      case e: ClassCastException =>
        throw new EntityMapperException(
          s"Incompatible serializer class for column '$colName'.", e)
      case NonFatal(e) =>
        throw new EntityMapperException(
          s"Exception when serializing value for column '$colName' for entity '$targetId'.", e)
    }
  }

  def insert(targetId: Any, colName: String, serializer: Any, value: Any, ttlSeconds: Option[Int])
  : Unit = {
    val bytes = serialize(targetId, colName, serializer, value)
    type Integer = java.lang.Integer
    val nullableTttl: Integer  = ttlSeconds.map(i => i: Integer).orNull
    mutation.putColumn(colName, bytes, nullableTttl)
  }

  def remove(targetId: Any, colName: String): Unit = {
    mutation.deleteColumn(colName)
  }
}
