import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}

import java.time.{LocalDate, LocalTime}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class ReservePompanoTeeTime(seleniumService: SeleniumService, skipReservation: Boolean) {

  // CONSTANTS
  val loginUrl =
    "https://flpompanobeachgolfweb.myvscloud.com/webtrac/web/login.html"
  val searchUrlBase =
    "https://flpompanobeachgolfweb.myvscloud.com/webtrac/web/search.html"

  val logoutUrl =
    "https://flpompanobeachgolfweb.myvscloud.com/webtrac/web/logout.html"

  def loginAndReserveTeeTime(
                              userName: String,
                              password: String,
                              daysInAdvance: Int,
                              numberOfGolfers: Int,
                              startTime: Option[String]
                            ): Try[String] = {

    val dateToReserve = LocalDate.now().plusDays(daysInAdvance)
    val formattedDate: String =
      dateToReserve.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

    def encodeTime(time: LocalTime): String =
      "+" + time.format(DateTimeFormatter.ofPattern("h'%3A'mm+a"))

    val timeToReserveTry = startTime match {
      case Some(time) => parseTime(time)
      case None => Success(LocalTime.of(6, 30))
    }
    timeToReserveTry match {
      case Failure(ex) => Failure(new RuntimeException(s"Invalid time format: ${ex.getMessage}"))
      case Success(timeToReserve) =>
        val encodedStartTime = encodeTime(timeToReserve)
        val searchUrl =
          s"$searchUrlBase?Action=Start&SubAction=&secondarycode=1&numberofplayers=${numberOfGolfers}&begindate=${formattedDate}&begintime=${encodedStartTime}&numberofholes=18&reservee=SALink-23691352&display=Detail&module=GR&multiselectlist_value=&grwebsearch_buttonsearch=yes"
        var loggedIn = false
        var currentFunction = ""
        var remoteWebDriverOpt: Option[RemoteWebDriver] = None
        var webDriverWaitOpt: Option[WebDriverWait] = None
        val reservationResult = for {
          (driver, webDriverWait) <- Try {
            val a = seleniumService.createDriver
            val wait = seleniumService.createWebDriverWait(a)
            remoteWebDriverOpt = Some(a)
            webDriverWaitOpt = Some(wait)
            (a, wait)
          }
          _ <- Try {
            println("log in")
            currentFunction = "log in"
            login(userName, password)(driver, webDriverWait)
            println("check for active session")
            checkForActiveSessionAndHandle()(driver, webDriverWait)
            println("logged in")
            loggedIn = true
          }
          _ <- Try {
            currentFunction = "go to reservation page"
            driver.get(searchUrl)
            val search = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.id("grwebsearch_nextgenheader"))).getText
            if (!search.toLowerCase.contains("tee time search")) throw new RuntimeException("failed to navigate to search url")
            else println("navigated to search url")
          }
          _ <- Try {
            currentFunction = "set reservation details"
            setReservationDetails()(driver, webDriverWait)
            println("done set reservation details")
          }
          _ = Thread.sleep(5000)
          teeTime <- Try {
            currentFunction = "set tee time"
            val result = findTeeTime(numberOfGolfers, dateToReserve, timeToReserve)(driver, webDriverWait)
            println(s"find tee time success result=$result")
            result
          }

        } yield teeTime

        reservationResult match {
          case f@Failure(exception) =>
            remoteWebDriverOpt.foreach(driver => {
              if (loggedIn) Try(logout(driver))
              Try(driver.quit())
            })

            println(s"failure at: $currentFunction error=$exception")
            Failure(new RuntimeException(s"failure at: $currentFunction error=$exception"))
          case v@Success(value) =>
            remoteWebDriverOpt.foreach(driver => {
              if (loggedIn) Try(logout(driver))
              Try(driver.quit())
            })
            v
        }
    }
  }

  private def parseTime(timeStr: String): Try[LocalTime] = Try {
    LocalTime.parse(timeStr.toUpperCase(), DateTimeFormatter.ofPattern("h:mm a"))
  }

  private def findTeeTime(numberOfGolfers: Int, dateToReserve: LocalDate, timeToReserve: LocalTime)(implicit driver: RemoteWebDriver, webDriverWait: WebDriverWait): String = {
    val rows = driver
      .findElements(By.cssSelector("#grwebsearch_output_table tbody tr"))
      .asScala

    val validRows = rows.collect {
      case row: WebElement if {
        lazy val slotsCell = row
          .findElement(By.cssSelector("td[data-title='Open Slots']"))
          .getText
          .trim
          .toIntOption

        lazy val timeCell = row.findElement(By.cssSelector("td[data-title='Time']")).getText.trim
        lazy val addToCartLink = row
          .findElement(By.cssSelector("a[data-tooltip='Add To Cart']"))
          .getDomAttribute("href")
        lazy val pt = parseTime(timeCell)
        if (timeCell.isEmpty) {
          val found = row.findElement(By.cssSelector("td[data-title='Time']")).getText
          println(s"failed to get time found=$found row=${row.getAttribute("innerHTML")}")
          false
        } else if (pt.isFailure) {
          println(s"failed to parse time=$timeCell row=${row.getText} r=${row.getAttribute("innerHTML")}")
          false
        } else if (addToCartLink.isEmpty) {
          println("missing add to cart link")
          false
        } else if (!slotsCell.exists(_ >= numberOfGolfers)) {
          false
        } else true
      } =>
        val timeCell = row.findElement(By.cssSelector("td[data-title='Time']")).getText.trim
        val addToCartLink = row
          .findElement(By.cssSelector("a[data-tooltip='Add To Cart']"))
          .getAttribute("href")
        val teeTimeTry = parseTime(timeCell)
        val timeDiff = teeTimeTry.map(teeTime =>
          java.time.Duration.between(timeToReserve, teeTime).abs().toMinutes
        ).getOrElse(Long.MaxValue)

        (timeCell, addToCartLink, timeDiff)
    }.sortBy(_._3)

    validRows.zipWithIndex.foreach {
      case ((time, addToCartLink, diff), idx) =>
        println(s"idx=$idx tee time=$time diff=$diff")
    }
    validRows.headOption match {
      case Some((time, addToCartLink, diff)) =>
      println(s"closest available tee time=$time diff=$diff")
        println(s"Adding to cart via link: $addToCartLink")
        driver.get(addToCartLink)
        clickById("golfmemberselection_buttoncontinue")(webDriverWait)

        val confirmedMessage = s"confirmed tee time Palms date=$dateToReserve time=$time golfers=$numberOfGolfers"
        if (skipReservation) {
          "skipped " + confirmedMessage
        } else {
          clickById("processingprompts_buttononeclicktofinish")
          println("confirmed")
          confirmedMessage
        }
      case None =>
        println(s"Error: No available tee times found with ${numberOfGolfers} slots on ${dateToReserve}.")
        s"No available tee times found with golfers=${numberOfGolfers} on ${dateToReserve}."
    }
  }

  def logout(implicit driver: RemoteWebDriver) = {
    println("logout")
    driver.get(logoutUrl)
    println("done logout")
  }

  def login(username: String, password: String)(implicit driver: RemoteWebDriver, webDriverWait: WebDriverWait): Unit = {
    driver.get(loginUrl)
    webDriverWait
      .until(
        ExpectedConditions.presenceOfElementLocated(By.id("weblogin_username"))
      )
      .sendKeys(username)
    driver.findElement(By.id("weblogin_password")).sendKeys(password)
    driver.findElement(By.id("weblogin_buttonlogin")).click()
  }

  def clickById(buttonId: String)(implicit webDriverWait: WebDriverWait) = {
    val button =
      webDriverWait.until(
        ExpectedConditions.elementToBeClickable(By.id(buttonId))
      )
    button.click()
  }

  def checkForActiveSessionAndHandle()(implicit driver: RemoteWebDriver, webDriverWait: WebDriverWait) = {
    if (driver.getCurrentUrl.contains("/splash.html")) {

    } else if (driver.getCurrentUrl.contains("/login.html")) {
      val warningHeader = webDriverWait.until(
        ExpectedConditions.presenceOfElementLocated(
          By.cssSelector("h1.page-header")
        )
      )
      if (warningHeader.getText.contains("Active Session Alert")) {
        println("active session found, handling")
        clickById("loginresumesession_buttoncontinue")
        webDriverWait.until(ExpectedConditions.urlContains("/splash.html"))
      } else {
        val errorMessage = driver.findElement(By.cssSelector("div.message.error"))
        val errorText = errorMessage.findElement(By.tagName("p")).getText
        println(errorText)
        throw new RuntimeException(errorText)
      }
    } else throw new RuntimeException("failed to verify login: url not at /splash or /login")
  }

  def setReservationDetails()(implicit driver: RemoteWebDriver, webDriverWait: WebDriverWait) = {
    val selectedText = webDriverWait
      .until(
        ExpectedConditions.visibilityOfElementLocated(
          By.cssSelector("#secondarycode_vm_1_button .combobox__text")
        )
      )
      .getText

    if (!selectedText.contains("Palms")) {
      println("need to set course to Palms")
      clickById("secondarycode_vm_1_button")
      val palmsCourseOption = webDriverWait.until(
        ExpectedConditions.elementToBeClickable(
          By.xpath(
            "//li[@role='option' and .//span[contains(text(), 'Palms Course')]]"
          )
        )
      )
      palmsCourseOption.click()

      val selectedText = webDriverWait
        .until(
          ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("#secondarycode_vm_1_button .combobox__text")
          )
        )
        .getText
      assert(
        selectedText == "Palms Course",
        s"Expected 'Palms Course' but got '$selectedText'"
      )
    }

    clickById("grwebsearch_buttonsearch")
  }

}