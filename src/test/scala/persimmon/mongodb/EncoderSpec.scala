package persimmon.mongodb

import org.mongodb.scala.MongoClient
import org.scalatest.WordSpec
import persimmon.Persistable
import persimmon.mongodb.MongoDao.NotFound

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class EncoderSpec extends WordSpec {
  import EncoderSpec._
  val addr = Address("123 Happy Lane", "22042")
  val tim = Person("tim", 35, addr, Some(Person("hannah", 36, Address("7549 Rainbow Road", "91345"))), Person("bob", 2, addr) :: Nil)

  "Persistable object" when {
    "encoded" should {
      "encode to correctly" in {
        val reflected = Encoder.encode(tim).flatMap(Encoder.decode[Person])
        assert(reflected.exists(_ == tim))
      }
    }

    "created" should {
      "save to MongoDB correctly" in {

        val mongoClient = MongoClient("mongodb://localhost")
        implicit val db = mongoClient.getDatabase("persimmontest")
        implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

        val dao = new Dao[Person] {
          override val collectionName: String = "person"
        }

        val result = for {
          _ <- dao.delete(tim.name)
          _ <- dao.create(tim)
          obj <- dao.get(tim.name)
        } yield obj

        val reflected = Await.result(result, Duration.Inf)
        assert(tim == reflected)

        val failed = dao.delete(tim.name).flatMap(_ => dao.get(tim.name))
        assertThrows[NotFound] {
          Await.result(failed, Duration.Inf)
        }
      }
    }

  }



}

object EncoderSpec {
  case class Address(street: String, zip: String)
  case class Person(name: String, age: Int, address: Address, spouse: Option[Person] = None, children: List[Person] = Nil) extends Persistable
}

