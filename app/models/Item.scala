package models

import java.time.ZonedDateTime

import jp.t2v.lab.play2.pager.{ OrderType, Sortable }
import scalikejdbc._
import jsr310._
import skinny.orm._

/**
  * Item
  */
case class Item(id: Option[Long],
                code: String,
                name: String,
                url: String,
                imageUrl: String,
                price: Int,
                createAt: ZonedDateTime = ZonedDateTime.now(),
                updateAt: ZonedDateTime = ZonedDateTime.now())

object Item extends SkinnyCRUDMapper[Item] {

  override val tableName = "items"

  override def defaultAlias: Alias[Item] = createAlias("i")

  override def extract(rs: WrappedResultSet, n: ResultName[Item]): Item =
    autoConstruct(rs, n)

  private def toNamedValues(record: Item): Seq[(Symbol, Any)] = Seq(
    'code     -> record.code,
    'name     -> record.name,
    'url      -> record.url,
    'imageUrl -> record.imageUrl,
    'price    -> record.price,
    'createAt -> record.createAt,
    'updateAt -> record.updateAt
  )

  def create(item: Item)(implicit session: DBSession): Long =
    createWithAttributes(toNamedValues(item): _*)

  def update(item: Item)(implicit session: DBSession): Int =
    updateById(item.id.get).withAttributes(toNamedValues(item): _*)

  implicit object sortable extends Sortable[Item] {
    override val default: (String, OrderType) = ("id", OrderType.Descending)
    override val defaultPageSize: Int         = 10
    override val acceptableKeys: Set[String]  = Set("id")
  }
}
