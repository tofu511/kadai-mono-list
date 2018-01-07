package services

import models.Item
import scalikejdbc.{ AutoSession, DBSession }

import scala.concurrent.Future
import scala.util.Try

trait ItemService {

  def searchItems(keywordOpt: Option[String]): Future[Seq[Item]]

  def getItemByCode(itemCode: String)(implicit dBSession: DBSession = AutoSession): Future[Option[Item]]

  def getItemAndCreateByCode(itemCode: String)(implicit dBSession: DBSession = AutoSession): Future[Item]

  def getItemsByUserId(userId: Long)(implicit dBSession: DBSession = AutoSession): Try[Seq[Item]]

  def getItemById(itemId: Long)(implicit dBSession: DBSession = AutoSession): Future[Option[Item]]

  def getLatestItems(limit: Int = 20): Try[Seq[Item]]
}
