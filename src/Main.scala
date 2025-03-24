import cask._

import scala.util.{Failure, Success}
object ReservePompanoHttpServer extends MainRoutes {


  override def host="0.0.0.0"

  @cask.get("/health")
  def hello() = "healthy"

    @cask.postJson("/reservation")
    def makeReservation (
            userName:String,
            password:String,
            daysInAdvance:Int,
            numberOfGolfers:Int,
            startTime: Option[String]
  ): Response[String] = {
          val result =
            AppWiring.reservationService.loginAndReserveTeeTime(
              userName = userName,
              password = password,
              daysInAdvance = daysInAdvance,
              numberOfGolfers = numberOfGolfers,
              startTime = startTime
            ) match {
            case Failure(exception) =>
              println(s"failed to reserve error=${exception.getMessage}")
              cask.Response.apply(exception.getMessage, 500)
            case Success(value) => cask.Response(value)
          }
      println(s"make reservation $userName result=$result")
      result
    }

  initialize()


}

object Main extends App {
  println("start services")
   import AppWiring._
  ReservePompanoHttpServer.main(Array.empty[String])
  println("Started HTTP Server...")
//  sys.addShutdownHook(selinumService.stop)
}
