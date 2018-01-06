package models

import java.time.ZonedDateTime

import jp.t2v.lab.play2.pager.{ OrderType, Sortable }
import scalikejdbc._, jsr310._
import skinny.orm._

/**
  * User
  */
case class User(id: Option[Long],
                name: String,
                email: String,
                password: String,
                createAt: ZonedDateTime = ZonedDateTime.now(),
                updateAt: ZonedDateTime = ZonedDateTime.now())

object User extends SkinnyCRUDMapper[User] {

  override val tableName = "users"

  override def defaultAlias: Alias[User] = createAlias("u")

  override def extract(rs: WrappedResultSet, n: ResultName[User]): User =
    autoConstruct(rs, n)

  private def toNamedValues(record: User): Seq[(Symbol, Any)] = Seq(
    'name     -> record.name,
    'email    -> record.email,
    'password -> record.password,
    'createAt -> record.createAt,
    'updateAt -> record.updateAt
  )

  def create(user: User)(implicit session: DBSession): Long =
    createWithAttributes(toNamedValues(user): _*)

  def update(user: User)(implicit session: DBSession): Int =
    updateById(user.id.get).withAttributes(toNamedValues(user): _*)

  implicit object sortable extends Sortable[User] {
    override val default: (String, OrderType) = ("id", OrderType.Descending)
    override val defaultPageSize: Int         = 10
    override val acceptableKeys: Set[String]  = Set("id")
  }

}
