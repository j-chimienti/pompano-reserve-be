object TwilioApi {
  val accountSid = "AC054e39b10cb3c0adce2ba54956fc1d0e"
  val accountSidTest = "AC0cd05fc8fe2cee61b8b64b027416573a"
  val authToken = "5890f07c46a430f7df30a60742997d1a"
  val authTokenTest = "68160e28b4b9c8b9b02fbccb4eba2c99"
  val testNumberFrom = "+15005550006"
  val numberFrom = "+18449890447"

  def sendSmsTest(to: String, body: String) =
    sendSms(accountSidTest, authTokenTest, testNumberFrom, to, body)
  def sendSms(to: String, body: String): Unit =
    sendSms(accountSid, authToken, numberFrom, to, body)

  /**
   * curl 'https://api.twilio.com/2010-04-01/Accounts/AC054e39b10cb3c0adce2ba54956fc1d0e/Messages.json' -X POST \
   * --data-urlencode 'To=+13524034722' \
   * --data-urlencode 'From=+18449890447' \
   * --data-urlencode 'Body=testting' \
   * -u AC054e39b10cb3c0adce2ba54956fc1d0e:5890f07c46a430f7df30a60742997d1a
   *
   * @param accountSid
   * @param authToken
   * @param from
   * @param to
   * @param body
   */
  private def sendSms(
      accountSid: String,
      authToken: String,
      from: String,
      to: String,
      body: String
  ): Unit = {
    val request = requests
      .post(
        s"https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json",
        auth = (accountSid, authToken),
        data = Map(
          "From" -> from,
          "To" -> to,
          "Body" -> body
        )
      )
    println(request.statusCode)
    println(request.text())

  }
}

object RunTwilio extends App {
  TwilioApi.sendSms("3524034722", "testing")
}