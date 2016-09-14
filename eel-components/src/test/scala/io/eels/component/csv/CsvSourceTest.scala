package io.eels.component.csv

import java.nio.file.Paths

import io.eels.schema.{Field, FieldType, Schema}
import org.scalatest.{Matchers, WordSpec}

class CsvSourceTest extends WordSpec with Matchers {

  "CsvSource" should {
    "read schema" in {
      val file = getClass.getResource("/io/eels/component/csv/csvtest.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).schema() shouldBe Schema(
        Field("a", FieldType.String, true),
        Field("b", FieldType.String, true),
        Field("c", FieldType.String, true)
      )
    }
    "support null cell value option as null" in {
      val file = getClass.getResource("/io/eels/component/csv/csvwithempty.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withNullValue(null).toFrame(1).toSet().map(_.values) shouldBe
        Set(Vector("1", null, "3"))
    }
    "support null cell value replacement value" in {
      val file = getClass.getResource("/io/eels/component/csv/csvwithempty.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withNullValue("foo").toFrame(1).toSet().map(_.values) shouldBe
        Set(Vector("1", "foo", "3"))
    }
    "read from path" in {
      val file = getClass.getResource("/io/eels/component/csv/csvtest.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withHeader(Header.FirstRow).toFrame(1).size() shouldBe 3
      CsvSource(path).withHeader(Header.None).toFrame(1).size() shouldBe 4
    }
    "allow specifying manual schema" in {
      val file = getClass.getResource("/io/eels/component/csv/csvtest.csv").toURI()
      val path = Paths.get(file)
      val schema = Schema(
        Field("test1", FieldType.String, true),
        Field("test2", FieldType.String, true),
        Field("test3", FieldType.String, true)
      )
      CsvSource(path).withSchema(schema).toFrame(1).schema() shouldBe schema
    }
    "support reading header" in {
      val file = getClass.getResource("/io/eels/component/csv/csvtest.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withHeader(Header.FirstRow).toFrame(1).toList().map(_.values).toSet shouldBe
        Set(Vector("e", "f", "g"), Vector("1", "2", "3"), Vector("4", "5", "6"))
    }
    "support skipping header" in {
      val file = getClass.getResource("/io/eels/component/csv/csvtest.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withHeader(Header.None).toFrame(1).toSet().map(_.values) shouldBe
        Set(Vector("a", "b", "c"), Vector("e", "f", "g"), Vector("1", "2", "3"), Vector("4", "5", "6"))
    }
    "support delimiters" in {
      val file = getClass.getResource("/io/eels/component/csv/psv.psv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withDelimiter('|').toFrame(1).toList().map(_.values).toSet shouldBe
        Set(Vector("e", "f", "g"))
      CsvSource(path).withDelimiter('|').withHeader(Header.None).toFrame(1).toSet().map(_.values) shouldBe
        Set(Vector("a", "b", "c"), Vector("e", "f", "g"))
    }
    "support comments for headers" in {
      val file = getClass.getResource("/io/eels/component/csv/comments.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withHeader(Header.FirstComment).schema() shouldBe Schema(
        Field("a", FieldType.String, true),
        Field("b", FieldType.String, true),
        Field("c", FieldType.String, true)
      )
      CsvSource(path).withHeader(Header.FirstComment).toFrame(1).toSet().map(_.values) shouldBe
        Set(Vector("1", "2", "3"), Vector("e", "f", "g"), Vector("4", "5", "6"))
    }
    "terminate if asking for first comment but no comments" in {
      val file = getClass.getResource("/io/eels/component/csv/csvtest.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withHeader(Header.FirstComment).schema() shouldBe Schema(
        Field("", FieldType.String, true)
      )
    }
    "support skipping corrupt rows" ignore {
      val file = getClass.getResource("/io/eels/component/csv/corrupt.csv").toURI()
      val path = Paths.get(file)
      CsvSource(path).withHeader(Header.FirstRow).toFrame(1).toList().map(_.values) shouldBe
        Seq(Vector("1", "2", "3"))
    }
  }
}
