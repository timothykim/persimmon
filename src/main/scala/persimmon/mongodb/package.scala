package persimmon

package object mongodb {
  type Dao[A <: Persistable] = mongodb.MongoDao[A]
  type NotFound = mongodb.MongoDao.NotFound
  type InternalError = mongodb.MongoDao.InternalError
}
