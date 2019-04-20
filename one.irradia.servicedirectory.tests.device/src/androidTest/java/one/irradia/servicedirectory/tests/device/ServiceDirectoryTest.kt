package one.irradia.servicedirectory.tests.device

import android.support.test.filters.MediumTest
import android.support.test.runner.AndroidJUnit4
import one.irradia.servicedirectory.api.ServiceDirectoryType
import one.irradia.servicedirectory.tests.ServiceDirectoryContract
import one.irradia.servicedirectory.vanilla.ServiceDirectory
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class ServiceDirectoryTest : ServiceDirectoryContract() {

  override fun logger(): Logger =
    LoggerFactory.getLogger(ServiceDirectoryTest::class.java)

  override fun serviceDirectory(): ServiceDirectoryType =
    ServiceDirectory.create()

}