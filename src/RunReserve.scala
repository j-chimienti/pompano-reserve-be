

import java.time.{DayOfWeek, LocalDate}

object RunReserve extends App {


  val day: String = "Sunday"
  private val startTime = None // Some("5:15 PM")
  private val numberOfGolfers = 4

  val userName = "jchimien@gmail.com"
  val password = "merrick!GHOST8beings"
  val selinumService = new SeleniumService(devEnv = true)
  val reservationService = new ReservePompanoTeeTime(selinumService, skipReservation = false)


  def daysUntilNext(dayOfWeek: String): Int = {
    val targetDay = DayOfWeek.valueOf(dayOfWeek.toUpperCase)
    val today = LocalDate.now()
    val daysUntil = targetDay.getValue - today.getDayOfWeek.getValue
    val days = if (daysUntil <= 0) daysUntil + 7 else daysUntil
    if (days > 6) {
      println("Cannot reserve more than 6 days in advance")
      System.exit(1)
    }
    days
  }
  val daysToAdvance = 7 //daysUntilNext(day)

  val result = reservationService.loginAndReserveTeeTime(
    userName = userName, password = password, daysInAdvance = daysToAdvance, numberOfGolfers = numberOfGolfers, startTime = startTime)

  println(result)

}