import com.google.inject.AbstractModule
import services.{ ItemService, ItemServiceImpl, UserService, UserServiceImpl }

class AppModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[UserService]).to(classOf[UserServiceImpl])
    bind(classOf[ItemService]).to(classOf[ItemServiceImpl])
  }

}
