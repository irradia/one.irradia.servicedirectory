package one.irradia.servicedirectory.tests.local

import one.irradia.servicedirectory.api.ServiceDirectoryType
import one.irradia.servicedirectory.tests.ServiceDirectoryContract
import one.irradia.servicedirectory.vanilla.ServiceDirectory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ServiceDirectoryTest : ServiceDirectoryContract() {

  override fun logger(): Logger =
    LoggerFactory.getLogger(ServiceDirectoryTest::class.java)

  override fun serviceDirectory(): ServiceDirectoryType =
    ServiceDirectory.create()

}