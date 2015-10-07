package com.pagerduty.eris.mapper

import java.nio.ByteBuffer

import com.netflix.astyanax.Serializer
import com.netflix.astyanax.model.{Column, ColumnList}
import com.pagerduty.mapper.{EntityMapperException, ResultAdapter}
import scala.util.control.NonFatal

/**
 * Eris implementation of [[ResultAdapter]]
 */
private[mapper] class ErisResultAdapter(result: ColumnList[String]) extends ResultAdapter {
  private def deserialize(targetId: Any, colName: String, serializer: Any, column: Column[String])
  : Any = {
    try {
      val usableSerializer = serializer.asInstanceOf[Serializer[Any]]
      column.getValue(usableSerializer)
    }
    catch { // Adding columnName to exception message to simplify debugging.
      case e: ClassCastException =>
        throw new EntityMapperException(
          s"Incompatible serializer class for column '$colName'.", e)
      case NonFatal(e) =>
        throw new EntityMapperException(
          s"Exception when de-serializing value for column '$colName' for entity '$targetId'.", e)
    }
  }
  override def get(targetId: Any, colName: String, serializer: Any): Option[Any] = {
    Option(result.getColumnByName(colName)).map { column =>
      deserialize(targetId, colName, serializer, column)
    }
  }
}
