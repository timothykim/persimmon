Quick Start
===========

Prerequisite:

* Scala 2.11


In your build.sbt:

````
libraryDependencies += "com.github.timothykim" %% "persimmon" % "0.7.0"
````

Then in code:

````
import persimmon.mongodb._

case class Person(name: String, age: Int)

val mongoClient = MongoClient("mongodb://localhost")
implicit val db = mongoClient.getDatabase("persimmontest")
implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

val dao = new Dao[Person] {
  val collectionName: String = "person"
}

Await.result(
  dao.create(Person("Tim", 42)),
  Duration.Inf)

val result = dao.get("Tim")

result.foreach(person => println(person))   // Person(Tim,42)

result.foreach(_ => dao.delete("Tim"))

dao.get("Tim")  // Will return Failure with NotFound object
````


