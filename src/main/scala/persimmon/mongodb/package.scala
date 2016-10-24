package persimmon

package object mongodb {
  type Dao[A <: Persistable] = mongodb.MongoDao[A]
}
