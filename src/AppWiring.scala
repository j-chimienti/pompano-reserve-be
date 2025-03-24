object AppWiring {
  val devEnv = sys.env.getOrElse("IS_DEVELOPMENT","false") == "true"
  val skipReservation = sys.env.getOrElse("SKIP_RESERVATION", "false") == "true"
//  assert(skipReservation)
  val selinumService = new SeleniumService(devEnv)
  val reservationService = new ReservePompanoTeeTime(selinumService, skipReservation)
}