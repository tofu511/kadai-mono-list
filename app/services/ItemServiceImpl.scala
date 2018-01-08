package services
import java.time.ZonedDateTime
import javax.inject.{ Inject, Singleton }

import akka.actor.ActorSystem
import models.{ Item, ItemUser, WantHaveType }
import play.api.Configuration
import play.api.libs.concurrent.ActorSystemProvider
import com.github.j5ik2o.rakutenApi.itemSearch.{
  ImageFlagType,
  RakutenItemSearchAPI,
  RakutenItemSearchAPIConfig,
  Item => RakutenItem
}
import scalikejdbc.{ DBSession, sqls, _ }
import scalikejdbc.interpolation.SQLSyntax.count

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

@Singleton
class ItemServiceImpl @Inject()(configuration: Configuration, actorSystemProvider: ActorSystemProvider)
    extends ItemService {

  private implicit val system: ActorSystem = actorSystemProvider.get

  import system.dispatcher

  private val config = RakutenItemSearchAPIConfig(
    endPoint = configuration.getString("rakuten.endPoint").get,
    timeoutForToStrict = configuration.getInt("rakuten.timeoutForToStrictInSec").get seconds,
    applicationId = configuration.getString("rakuten.applicationId").get,
    affiliateId = configuration.getString("rakuten.affiliateId")
  )

  private val rakutenItemSearchAPI = new RakutenItemSearchAPI(config)

  override def searchItems(keywordOpt: Option[String]): Future[Seq[Item]] = {
    keywordOpt
      .map { keyword =>
        rakutenItemSearchAPI
          .searchItems(
            keyword = Some(keyword),
            hits = Some(20),
            imageFlag = Some(ImageFlagType.HasImage)
          )
          .map(_.Items.map(convertToItem))
      }
      .getOrElse(Future.successful(Seq.empty))
  }

  // 楽天の検索結果にあるitemCodeからItemを検索
  // あればそれを返し、なければcreateItemFromRakutenItemメソッドで新しく作る
  // ただし、保存はしない
  private def convertToItem(rakutenItem: RakutenItem): Item = {
    Item.allAssociations
      .findBy(sqls.eq(Item.defaultAlias.code, rakutenItem.value.itemCode))
      .getOrElse(
        createItemFromRakutenItem(rakutenItem)
      )
  }

  override def getItemByCode(itemCode: String)(implicit dBSession: DBSession): Future[Option[Item]] = Future {
    Item.allAssociations.findBy(sqls.eq(Item.defaultAlias.code, itemCode))
  }

  override def getItemAndCreateByCode(itemCode: String)(implicit dBSession: DBSession): Future[Item] = {
    getItemByCode(itemCode).flatMap {
      case Some(item) => Future.successful(item)
      case None =>
        searchItemByItemCode(itemCode).map { item =>
          val id = createIfNot(item).get
          item.copy(id = Some(id))
        }
    }
  }

  // itemCodeから楽天商品を検索する
  private def searchItemByItemCode(itemCode: String): Future[Item] =
    rakutenItemSearchAPI.searchItems(itemCode = Some(itemCode)).map(_.Items.head).map(createItemFromRakutenItem)

  // 楽天の検索結果をItemに変換するメソッド
  private def createItemFromRakutenItem(rakutenItem: RakutenItem): Item = {
    val now = ZonedDateTime.now()
    Item(
      id = None,
      code = rakutenItem.value.itemCode,
      name = rakutenItem.value.itemName,
      url = rakutenItem.value.itemUrl.toString,
      imageUrl = rakutenItem.value.mediumImageUrls.head.value.toString.replace("?_ex=128x128", ""),
      price = rakutenItem.value.itemPrice.toInt,
      createAt = now,
      updateAt = now
    )
  }

//  private def create(item: Item)(implicit dBSession: DBSession): Try[Long] = Try {
//    Item.create(item)
//  }

  private def createIfNot(item: Item)(implicit dBSession: DBSession): Try[Long] = Try {
    val itemId     = Item.findBy(sqls.eq(Item.defaultAlias.code, item.code)).map(_.id).getOrElse(Some(0L)).get
    val hasCreated = itemId > 0L
    if (hasCreated)
      itemId
    else
      Item.create(item)
  }

  override def getItemsByUserId(userId: Long)(implicit dBSession: DBSession): Try[Seq[Item]] = Try {
    Item.allAssociations.findAllBy(
      sqls.eq(ItemUser.defaultAlias.userId, userId)
    )
  }

  override def getItemById(itemId: Long)(implicit dBSession: DBSession): Future[Option[Item]] = Future {
    Item.allAssociations.findById(itemId)
  }

  override def getLatestItems(limit: Int = 20): Try[Seq[Item]] = Try {
    Item.allAssociations
      .findAllWithLimitOffset(limit, orderings = Seq(Item.defaultAlias.updateAt.desc))
      .toVector
  }

  override def getItemsByRanking(`type`: WantHaveType.Value)(implicit dBSession: DBSession): Try[Seq[(Item, Int)]] =
    Try {
      val i  = Item.syntax("i")
      val iu = ItemUser.syntax("iu")
      withSQL {
        select(count(i.id), i.resultAll)
          .from(Item as i)
          .leftJoin(ItemUser as iu)
          .on(iu.itemId, i.id)
          .where
          .eq(iu.`type`, `type`.toString)
          .groupBy(i.id)
          .orderBy(count)
          .desc
      }.map(rs => (Item(i)(rs), rs.int(1))).list().apply().toVector
    }
}
