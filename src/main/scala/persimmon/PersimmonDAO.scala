package persimmon

import scala.concurrent.{ExecutionContext, Future}

trait Persistable

trait PersimmonDAO[A <: Persistable] {
  def create(obj: A)(implicit ec: ExecutionContext): Future[A]
  def update(obj: A)(implicit ec: ExecutionContext): Future[A]
  def save(obj: A)(implicit ec: ExecutionContext): Future[A]
  def get(id: String)(implicit ec: ExecutionContext): Future[A]
  def delete(id: String)(implicit ec: ExecutionContext): Future[Unit]
}
