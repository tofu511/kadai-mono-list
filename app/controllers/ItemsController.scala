package controllers

import javax.inject.{ Inject, Singleton }

import akka.pattern.AskTimeoutException
import com.github.j5ik2o.rakutenApi.itemSearch.ItemSearchException
import akka.util.Timeout
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc._
import services.{ ItemService, UserService }
import jp.t2v.lab.play2.auth.AuthenticationElement
import play.Logger
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

@Singleton
class ItemsController @Inject()(val itemService: ItemService,
                                val userService: UserService,
                                val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
    extends Controller
    with I18nSupport
    with AuthConfigSupport
    with AuthenticationElement {

  implicit val timeout = Timeout(20 seconds)

  private val searchItemForm: Form[Option[String]] = Form(
    "keyword" -> optional(text)
  )

  private def recoderHandler(
      currentUser: User,
      keywordOpt: Option[String]
  )(implicit requestHeader: RequestHeader): PartialFunction[Throwable, Result] = {
    case ex: ItemSearchException =>
      Logger.error(Messages("RakutenAPICallError"), ex)
      InternalServerError(
        views.html.items.index(currentUser,
                               searchItemForm.fill(keywordOpt).withGlobalError(Messages("RakutenAPICallError")),
                               Seq.empty)
      )
    case ex: AskTimeoutException =>
      Logger.error(Messages("RakutenAPICallError"), ex)
      InternalServerError(
        views.html.items
          .index(currentUser,
                 searchItemForm.fill(keywordOpt).withGlobalError(Messages("RakutenAPICallError")),
                 Seq.empty)
      )
  }

  private def searchItems(
      currentUser: User,
      keywordOpt: Option[String]
  )(implicit requestHeader: RequestHeader): Future[Result] = {
    itemService
      .searchItems(keywordOpt)
      .map { items =>
        Logger.debug(s"items = ${items}\n")
        Ok(views.html.items.index(currentUser, searchItemForm.fill(keywordOpt), items))
      }
      .recover(recoderHandler(currentUser, keywordOpt))
  }

  def index(keywordOpt: Option[String]): Action[AnyContent] = AsyncStack { implicit request =>
    searchItems(loggedIn, keywordOpt)
  }

}
