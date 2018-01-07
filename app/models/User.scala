package models

import java.time.ZonedDateTime

import jp.t2v.lab.play2.pager.{ OrderType, Sortable }
import scalikejdbc._
import jsr310._
import skinny.orm._
import skinny.orm.feature.CRUDFeatureWithId
import skinny.orm.feature.associations.HasManyAssociation

/**
  * User
  */
case class User(id: Option[Long] = None,
                name: String,
                email: String,
                password: String,
                createAt: ZonedDateTime = ZonedDateTime.now(),
                updateAt: ZonedDateTime = ZonedDateTime.now(),
                items: Seq[Item] = Seq.empty,
                wantItems: Seq[Item] = Seq.empty)

object User extends SkinnyCRUDMapper[User] {

  lazy val allAssociations: CRUDFeatureWithId[Long, User] = joins(allItemRef, wantItemsRef)

  lazy val allItemRef: HasManyAssociation[User] = hasManyThrough[Item](
    through = ItemUser, // 中間テーブルを指定
    many = Item,
    merge = (user, items) => user.copy(items = items)
  )

  lazy val wantItemsRef: HasManyAssociation[User] = hasManyThrough[ItemUser, Item](
    through = ItemUser -> ItemUser.createAlias("iu"), // 中間テーブルを指定
    throughOn = (user: Alias[User], itemUser: Alias[ItemUser]) => sqls.eq(user.id, itemUser.userId),
    many = Item -> Item.createAlias("i_in_u_want"),
    on = (itemUser: Alias[ItemUser], item: Alias[Item]) =>
      sqls.eq(itemUser.itemId, item.id).and.eq(itemUser.`type`, WantHaveType.Want.toString), // 結合条件を指定
    merge = (user, wantItems) => user.copy(wantItems = wantItems)
  )

  override def tableName = "users"

  override def defaultAlias: Alias[User] = createAlias("u")

  override def extract(rs: WrappedResultSet, n: ResultName[User]): User =
    autoConstruct(rs, n, "items", "wantItems")

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
