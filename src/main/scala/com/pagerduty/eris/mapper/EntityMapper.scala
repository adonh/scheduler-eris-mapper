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

import java.lang.annotation.Annotation

import com.netflix.astyanax.{ Serializer, ColumnListMutation }
import com.netflix.astyanax.model.ColumnList
import com.pagerduty.eris.ColumnModel
import com.pagerduty.eris.serializers.ValidatorClass
import com.pagerduty.mapper._

/**
 * Allows to read objects from Astyanax row results and write objects to Astyanax mutation batch.
 */
class EntityMapper[Id, Entity](
    entityClass: Class[Entity],
    registeredSerializers: Map[Class[_], Serializer[_]],
    customMappers: Map[Class[_ <: Annotation], Mapping => Mapping] = Map.empty
) {
  /**
   * Entity mapping that handles translation of individual fields.
   */
  protected val mapping = EntityMapping[Id, Entity](
    entityClass, registeredSerializers, customMappers
  )

  /**
   * The value of @Ttl annotation, if defined.
   *
   * @return TTL in seconds
   */
  def ttlSeconds: Option[Int] = mapping.ttlSeconds

  /**
   * Retrieves the value of @Id annotated field, if defined.
   *
   * @param entity the target entity
   * @return id
   */
  def getId(entity: Entity): Option[Id] = {
    if (mapping.isIdDefined) Some(mapping.getId(entity)) else None
  }

  /**
   * Write entity columns into mutation batch.
   *
   * @param targetId target entity id
   * @param entity target entity
   * @param mutation mutation batch
   * @param ttlSeconds ttl in seconds, defaults to entity @Ttl
   */
  def write(
    targetId: Id,
    entity: Entity,
    mutation: ColumnListMutation[String],
    ttlSeconds: Option[Int] = this.ttlSeconds
  ): Unit = {
    mapping.write(targetId, Some(entity), new ErisMutationAdapter(mutation), ttlSeconds)
  }

  /**
   * Read entity from row result.
   *
   * @param targetId target entity id
   * @param result row result
   * @return Some(entity) if present, None otherwise
   */
  def read(targetId: Id, result: ColumnList[String]): Option[Entity] = {
    val resultAdapter = new ErisResultAdapter(result)
    mapping.read(targetId, resultAdapter).map { entity =>
      if (mapping.isIdDefined) mapping.setId(entity, targetId)
      entity
    }
  }

  /**
   * A set of column declarations that can be used to generate schema.
   */
  lazy val columns: Set[ColumnModel] = {
    val res = for ((colName, serializer) <- mapping.serializersByColName) yield {
      val usableSerializer = serializer.asInstanceOf[Serializer[_]]
      val validatorClass = ValidatorClass(usableSerializer)
      ColumnModel(colName, false, validatorClass)
    }
    res.toSet
  }
}
