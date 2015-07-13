package com.pagerduty.eris.mapper

import java.nio.ByteBuffer
import java.util.logging.{Level, Logger}

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
