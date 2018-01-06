package controllers

import jp.t2v.lab.play2.auth.AuthConfig
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{ RequestHeader, Result }
import services.UserService

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect._

trait AuthConfigSupport extends AuthConfig {

  /**
    * ユーザを識別するIDの型です。
    */
  override type Id = String

  /**
    * アプリケーションで認証するユーザを表す型です。
    */
  override type User = models.User

  /**
    * 認可(権限チェック)を行う際に、アクション毎に設定するオブジェクトの型です。
    */
  override type Authority = Nothing

  /**
    * CacheからユーザIDを取り出すための ClassTag です。
    */
  override implicit val idTag: ClassTag[Id] = classTag[Id]

  val userService: UserService

  /**
    * セッションタイムアウトの時間(秒)です。
    */
  override def sessionTimeoutInSeconds: Int = 3600

  /**
    * SessionID Tokenの保存場所の設定です。
    */
  override lazy val tokenAccessor = new RememberMeTokenAccessor(sessionTimeoutInSeconds)

  /**
    * ユーザIDからUserブジェクトを取得するアルゴリズムを指定します。
    */
  override def resolveUser(id: String)(implicit context: ExecutionContext): Future[Option[User]] =
    Future {
      userService.findByEmail(id).get
    }

  /**
    * ログインが成功した際に遷移する先を指定します。
    */
  override def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
    Future.successful {
      Redirect(routes.HomeController.index())
    }

  /**
    * ログアウトが成功した際に遷移する先を指定します。
    */
  override def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
    Future.successful {
      Redirect(routes.HomeController.index())
    }

  /**
    * 認証が失敗した場合に遷移する先を指定します。
    */
  override def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
    Future.successful {
      Redirect(routes.AuthController.index())
    }

  /**
    * 認可(権限チェック)が失敗した場合に遷移する先を指定します。
    * (今回は利用しないので適当なレスポンスを返す)
    */
  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Nothing])(
      implicit context: ExecutionContext
  ): Future[Result] =
    Future.successful {
      Forbidden("no permission")
    }

  /**
    * 権限チェックのアルゴリズムを指定します。
    * (今回はロールを区別しないので常にtrueを返す)
    */
  override def authorize(user: User, authority: Nothing)(implicit context: ExecutionContext): Future[Boolean] =
    Future.successful {
      true
    }

}
