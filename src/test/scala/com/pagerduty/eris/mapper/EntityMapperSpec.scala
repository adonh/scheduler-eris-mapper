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

import com.pagerduty.eris.{ColumnModel, TimeUuid}
import com.pagerduty.eris.serializers._
import com.pagerduty.mapper.annotations._
import org.scalatest.{Matchers, FreeSpec}

package test {
  @Entity
  @Ttl(seconds = 40) case class SimpleEntityWithId(
      @Id id: TimeUuid,
      @Column(name = "f0") field0: String,
      @Column(name = "f1") field1: Int,
      transient: String) {
    def this() = this(null, "default0", 0, "defaultTransient")
  }

  @Entity case class SimpleEntityWithoutId(
      @Column(name = "f0") field0: String,
      @Column(name = "f1") field1: Int,
      transient: String) {
    def this() = this("default0", 0, "defaultTransient")
  }
}

class EntityMapperSpec extends FreeSpec with Matchers {
  "EntityMapper should" - {
    "detect when there is no TTL" in {
      type Entity = test.SimpleEntityWithoutId
      val mapper = new EntityMapper[TimeUuid, Entity](classOf[Entity], CommonSerializers)
      mapper.ttlSeconds shouldBe None
    }

    "detect TTL" in {
      type Entity = test.SimpleEntityWithId
      val mapper = new EntityMapper[TimeUuid, Entity](classOf[Entity], CommonSerializers)
      mapper.ttlSeconds shouldBe Some(40)
    }

    "auto-detect columns" in {
      type Entity = test.SimpleEntityWithId
      val mapper = new EntityMapper[TimeUuid, Entity](classOf[Entity], CommonSerializers)
      mapper.columns should contain allOf (ColumnModel[String]("f0"), ColumnModel[Int]("f1"))
    }
  }
}
