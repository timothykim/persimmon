package persimmon.mongodb

import cats.data.Xor
import org.json4s._
import org.mongodb.scala.Document
import org.mongodb.scala.bson._
import persimmon.Persistable

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

private[mongodb] object Encoder {

  implicit val formats = DefaultFormats

  case class EncoderException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

  private def injectId(json: JValue): Try[JObject] = json match {
    case JObject(fields) => Success(JObject(("_id" -> fields.head._2) :: fields))
    case _ => Failure(EncoderException(""))
  }

  private def toJValue(value: BsonValue): JValue = value match {
    case _: BsonUndefined => JNothing
    case _: BsonNull => JNull
    case str: BsonString => JString(str.getValue)
    case dbl: BsonDouble => JDouble(dbl.doubleValue())
    case long: BsonInt64 => JLong(long.getValue)
    case int: BsonInt32 => JInt(int.getValue)
    case bool: BsonBoolean => JBool(bool.getValue)
    case doc: BsonDocument => JObject(doc.entrySet().asScala.map(entry => entry.getKey -> toJValue(entry.getValue)).toList)
    case array: BsonArray => JArray(array.getValues.asScala.map(toJValue).toList)
  }

  private def toBson(value: JValue): BsonValue = value match {
    case JNothing => BsonUndefined()
    case JNull =>  BsonNull()
    case JString(string) => BsonString(string)
    case JDouble(double) => BsonNumber(double)
    case JDecimal(bigDecimal) => BsonNumber(bigDecimal.toDouble)
    case JLong(long) => BsonNumber(long)
    case JInt(bigInt) => BsonNumber(bigInt.toInt)
    case JBool(boolean) => BsonBoolean(boolean)
    case JObject(fields) => BsonDocument(fields.map { case (k,v) => k -> toBson(v) })
    case JArray(values) => BsonArray(values.map(toBson))
  }

  private def toDocument(json: JObject): Document = Document(toBson(json).asInstanceOf[BsonDocument])

  private def toJObject(doc: Document): JObject = toJValue(doc.toBsonDocument).asInstanceOf[JObject]

  private def toManifest[T:TypeTag]: Manifest[T] = {
    val tt = typeTag[T]
    val mirror = tt.mirror
    def toManifestRec(t: Type): Manifest[_] = {
      import scala.reflect.{ClassTag, ManifestFactory}
      val clazz = ClassTag[T](mirror.runtimeClass(t)).runtimeClass
      if (t.typeArgs.length == 1) {
        val arg = toManifestRec(t.typeArgs.head)
        ManifestFactory.classType(clazz, arg)
      } else if (t.typeArgs.length > 1) {
        val args = t.typeArgs.map(x => toManifestRec(x))
        ManifestFactory.classType(clazz, args.head, args.tail: _*)
      } else {
        ManifestFactory.classType(clazz)
      }
    }
    toManifestRec(tt.tpe).asInstanceOf[Manifest[T]]
  }

  def encode[A <: Persistable](obj: A): Xor[Throwable, Document] = {
    val document = for {
      json <- Try(Extraction.decompose(obj))
      jsonWithId <- injectId(json)
      doc <- Try(toDocument(jsonWithId))
    } yield doc
    Xor.fromTry(document)
  }

  def decode[A <: Persistable:TypeTag](doc: Document): Xor[Throwable, A] = {
    Xor.fromTry(Try(Extraction.extract[A](toJObject(doc))(formats, toManifest[A])))
  }
}
