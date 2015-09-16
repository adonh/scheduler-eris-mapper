package com.pagerduty.eris.mapper

import com.pagerduty.eris.schema.SchemaLoader
import com.pagerduty.eris._
import org.scalatest.{Matchers, FreeSpec}
import com.pagerduty.mapper.annotations._
import com.pagerduty.eris.serializers._
import FutureConversions._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global


package test {
  @Entity @Ttl(seconds = 40) case class SimpleEntityWithId(
      @Id id: TimeUuid,
      @Column(name = "f0") field0: String,
      @Column(name = "f1") field1: Int,
      transient: String)
  {
    def this() = this(null, "default0", 0, "defaultTransient")
  }

  @Entity case class SimpleEntityWithoutId(
      @Column(name = "f0") field0: String,
      @Column(name = "f1") field1: Int,
      transient: String)
  {
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
      mapper.columns should contain allOf(ColumnModel[String]("f0"), ColumnModel[Int]("f1"))
    }

    "when there is Id" - {
      type Entity = test.SimpleEntityWithId
      val mapper = new EntityMapper[TimeUuid, Entity](classOf[Entity], CommonSerializers)
      val id = TimeUuid()
      val entity = new Entity(id, "val1", 1, "t1")

      "retrieve Id" in {
        val foundId = mapper.getId(entity)
        foundId shouldBe Some(id)
      }

      "write/read entity" in {
        val cluster = TestClusterCtx.cluster
        val keyspace = cluster.getKeyspace("EntityMapperSpecId")
        val cfModel = ColumnFamilyModel[TimeUuid, String, String](
          keyspace, "entityCf", columns = mapper.columns)
        val schemaLoader = new SchemaLoader(cluster, Set(cfModel.columnFamilyDef(cluster)))
        schemaLoader.dropSchema()
        schemaLoader.loadSchema()

        val mutationBatch = keyspace.prepareMutationBatch()
        val rowMutation = mutationBatch.withRow(cfModel.columnFamily, id)
        mapper.write(id, entity, rowMutation)
        Await.result(mutationBatch.executeAsync(), 5.seconds)

        val query = keyspace.prepareQuery(cfModel.columnFamily).getKey(id)
        val readFuture = query.executeAsync().map(res => mapper.read(id, res.getResult))
        val loaded = Await.result(readFuture, 5.seconds)

        loaded shouldBe Some(entity.copy(transient = "defaultTransient"))
      }
    }

    "when there is no Id" - {
      type Entity = test.SimpleEntityWithoutId
      val mapper = new EntityMapper[TimeUuid, Entity](classOf[Entity], CommonSerializers)
      val id = TimeUuid()
      val entity = new Entity("val1", 1, "t1")

      "return None for Id" in {
        val foundId = mapper.getId(entity)
        foundId shouldBe None
      }

      "write/read entity" in {
        val cluster = TestClusterCtx.cluster
        val keyspace = cluster.getKeyspace("EntityMapperSpecId")
        val cfModel = ColumnFamilyModel[TimeUuid, String, String](
          keyspace, "entityCf", columns = mapper.columns)
        val schemaLoader = new SchemaLoader(cluster, Set(cfModel.columnFamilyDef(cluster)))
        schemaLoader.dropSchema()
        schemaLoader.loadSchema()

        val mutationBatch = keyspace.prepareMutationBatch()
        val rowMutation = mutationBatch.withRow(cfModel.columnFamily, id)
        mapper.write(id, entity, rowMutation)
        Await.result(mutationBatch.executeAsync(), 5.seconds)

        val query = keyspace.prepareQuery(cfModel.columnFamily).getKey(id)
        val readFuture = query.executeAsync().map(res => mapper.read(id, res.getResult))
        val loaded = Await.result(readFuture, 5.seconds)

        loaded shouldBe Some(entity.copy(transient = "defaultTransient"))
      }
    }
  }
}
