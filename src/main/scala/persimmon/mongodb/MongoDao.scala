package persimmon.mongodb

import cats.data.Xor
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import persimmon.{PersimmonDAO, Persistable}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag

object MongoDao {
  abstract class PersimmonMongoException(msg: String, cause: Throwable = null) extends Exception(msg, cause)
  case class InternalError(msg: String, cause: Throwable = null) extends PersimmonMongoException(msg, cause)
  case class NotFound(msg: String, cause: Throwable = null) extends PersimmonMongoException(msg, cause)
}

abstract class MongoDao[A <: Persistable:TypeTag](implicit db: MongoDatabase) extends PersimmonDAO[A] {
  import Encoder._
  import MongoDao._

  val collectionName: String

  private def option2future[B](opt: Option[B], error: Throwable): Future[B] = opt match {
    case Some(obj) => Future.successful(obj)
    case None => Future.failed(error)
  }
  private def xor2future[C](xor: Xor[Throwable, C]): Future[C] = xor match {
    case Xor.Left(error) => Future.failed(error)
    case Xor.Right(obj) => Future.successful(obj)
  }

  def create(obj: A)(implicit ec: ExecutionContext): Future[A] = for {
    document <- xor2future(encode(obj))
    _ <- db.getCollection(collectionName).insertOne(document).toFuture()
  } yield obj

  def update(obj: A)(implicit ec: ExecutionContext): Future[A] = for {
    document <- xor2future(encode(obj))
    id <- option2future(document.get("_id"), InternalError("Encoder failure.", EncoderException("_id field was not created.")))
    _ <- db.getCollection(collectionName).updateOne(Document("_id" -> id),document).toFuture()
  } yield obj

  def save(obj: A)(implicit ec: ExecutionContext): Future[A] = create(obj).recoverWith { case _ => update(obj) }

  def get(id: String)(implicit ec: ExecutionContext): Future[A] = for {
    seq <- db.getCollection(collectionName).find(Document("_id" -> id)).toFuture()
    document <- option2future(seq.headOption, NotFound(s"Object with $id doesn't exist."))
    obj <- xor2future(decode[A](document))
  } yield obj

  def delete(id: String)(implicit ec: ExecutionContext): Future[Unit] = db.getCollection(collectionName).deleteOne(Document("_id" -> id)).toFuture().map(_ => {})
}
