import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.WebDriverWait

import java.net.URL

class SeleniumService(devEnv: Boolean) {

  def createDriver: RemoteWebDriver = {
    if (devEnv) {
      println("building chrome driver")
      new ChromeDriver()
    }
    else {
      println("building remote web driver")
      val options = new ChromeOptions()
      new RemoteWebDriver(new URL("http://selenium:4444/wd/hub"), options)
    }
  }
 def createWebDriverWait(driver: RemoteWebDriver) = new WebDriverWait(driver, java.time.Duration.ofSeconds(10))

}