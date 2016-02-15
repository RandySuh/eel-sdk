package io.eels.component.hive

import com.sksamuel.scalax.io.Using
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.eels.component.parquet.ParquetLogMute
import io.eels.{FrameSchema, Reader, Source}
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hive.conf.HiveConf
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient
import org.apache.hadoop.hive.metastore.api.Table

import scala.collection.JavaConverters._

case class HiveSource(db: String, table: String, partitionExprs: List[PartitionExpr] = Nil)
                     (implicit fs: FileSystem, hive: HiveConf)
  extends Source
    with StrictLogging
    with Using {

  ParquetLogMute()

  def withPartition(name: String, value: String): HiveSource = withPartition(name, "=", value)
  def withPartition(name: String, op: String, value: String): HiveSource = {
    val expr = op match {
      case "=" => PartitionEquals(name, value)
      case ">" => PartitionGt(name, value)
      case ">=" => PartitionGte(name, value)
      case "<" => PartitionLt(name, value)
      case "<=" => PartitionLte(name, value)
      case _ => sys.error(s"Unsupported op $op")
    }
    copy(partitionExprs = partitionExprs :+ expr)
  }

  private def createClient: HiveMetaStoreClient = new HiveMetaStoreClient(hive)

  override def schema: FrameSchema = {
    using(createClient) { client =>

      val s = client.getSchema(db, table).asScala
      logger.debug("Loaded hive schema " + s.mkString(", "))

      val frameSchema = FrameSchemaFn(s)
      logger.debug("Generated frame schema=" + frameSchema)

      frameSchema
    }
  }

  private def dialect(t: Table): HiveDialect = {

    val format = t.getSd.getInputFormat
    logger.debug(s"Table format is $format")

    val dialect = HiveDialect(format)
    logger.debug(s"HiveDialect is $dialect")

    dialect
  }

  override def readers: Seq[Reader] = {

    val (schema, dialect, paths) = using(createClient) { client =>
      val t = client.getTable(db, table)
      val schema = this.schema
      val dialect = this.dialect(t)
      val paths = HiveFileExplorer(t, partitionExprs)
      (schema, dialect, paths)
    }

    paths.map { path =>
      new Reader {
        ParquetLogMute()
        lazy val iterator = dialect.iterator(path, schema)
        override def close(): Unit = () // todo close dialect
      }
    }
  }
}