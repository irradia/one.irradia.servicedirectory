package one.irradia.servicedirectory.tests

import one.irradia.servicedirectory.api.ServiceConfigurationException
import one.irradia.servicedirectory.api.ServiceDirectoryType
import org.hamcrest.core.AllOf
import org.hamcrest.core.StringContains
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger

abstract class ServiceDirectoryContract {

  abstract fun logger(): Logger

  abstract fun serviceDirectory(): ServiceDirectoryType

  class A
  class B
  class C

  @JvmField
  @Rule
  val expectedException = ExpectedException.none()

  private lateinit var logger: Logger

  @Before
  fun testSetup() {
    this.logger = this.logger()
  }

  @Test
  fun testRegisterTwice() {
    val registry = this.serviceDirectory()

    registry.registerService(A::class.java) {
      this.logger.debug("construct A")
      A()
    }

    this.expectedException.expect(ServiceConfigurationException::class.java)
    registry.registerService(A::class.java) {
      this.logger.debug("construct A")
      A()
    }
  }

  @Test
  fun testInitOnceRequired() {
    val acount = AtomicInteger(0)
    val bcount = AtomicInteger(0)
    val registry = this.serviceDirectory()

    registry.registerService(A::class.java) {
      this.logger.debug("construct A")
      acount.incrementAndGet()
      A()
    }

    registry.registerService(B::class.java) {
      this.logger.debug("construct B")
      registry.requireService(A::class.java)
      bcount.incrementAndGet()
      B()
    }

    val b0 =
      registry.requireService(B::class.java)
    val b1 =
      registry.requireService(B::class.java)

    Assert.assertEquals(1, acount.get())
    Assert.assertEquals(1, bcount.get())
  }

  @Test
  fun testInitOnceOptional() {
    val acount = AtomicInteger(0)
    val bcount = AtomicInteger(0)
    val registry = this.serviceDirectory()

    registry.registerService(A::class.java) {
      this.logger.debug("construct A")
      acount.incrementAndGet()
      A()
    }

    registry.registerService(B::class.java) {
      this.logger.debug("construct B")
      registry.requireService(A::class.java)
      bcount.incrementAndGet()
      B()
    }

    val b0 =
      registry.optionalService(B::class.java)
    val b1 =
      registry.optionalService(B::class.java)

    Assert.assertEquals(1, acount.get())
    Assert.assertEquals(1, bcount.get())
  }

  @Test
  fun testInitServiceLoaderReuse() {
    val registry = this.serviceDirectory()

    val b0 =
      registry.requireService(AvailableService::class.java)
    val b1 =
      registry.requireService(AvailableService::class.java)

    Assert.assertSame(b0, b1)
  }

  @Test
  fun testInitServiceLoaderReuseOptional() {
    val registry = this.serviceDirectory()

    val b0 =
      registry.optionalService(AvailableService::class.java)
    val b1 =
      registry.optionalService(AvailableService::class.java)

    Assert.assertSame(b0, b1)
  }

  interface FakeService

  @Test
  fun testInitServiceLoaderMissingRequired() {
    val registry = this.serviceDirectory()

    this.expectedException.expect(ServiceConfigurationException::class.java)
    registry.requireService(FakeService::class.java)
  }

  @Test
  fun testInitServiceLoaderMissingOptional() {
    val registry = this.serviceDirectory()
    registry.optionalService(FakeService::class.java)
  }

  @Test
  fun testInitNever() {
    val acount = AtomicInteger(0)
    val bcount = AtomicInteger(0)
    val registry = this.serviceDirectory()

    registry.registerService(A::class.java) {
      this.logger.debug("construct A")
      acount.incrementAndGet()
      A()
    }

    registry.registerService(B::class.java) {
      this.logger.debug("construct B")
      registry.requireService(A::class.java)
      bcount.incrementAndGet()
      B()
    }

    Assert.assertEquals(0, acount.get())
    Assert.assertEquals(0, bcount.get())
  }

  @Test
  fun testMissing() {
    val registry = this.serviceDirectory()

    this.expectedException.expect(ServiceConfigurationException::class.java)
    registry.requireService(A::class.java)
  }

  @Test
  fun testInitCircularRequired() {
    val registry = this.serviceDirectory()

    registry.registerService(A::class.java) {
      this.logger.debug("construct B")
      registry.requireService(B::class.java, caller = A::class.java)
      A()
    }

    registry.registerService(B::class.java) {
      this.logger.debug("construct B")
      registry.requireService(A::class.java, caller = B::class.java)
      B()
    }

    this.expectedException.expect(ServiceConfigurationException::class.java)
    this.expectedException.expectMessage(
      AllOf(mutableListOf(
          StringContains.containsString(A::class.java.canonicalName),
          StringContains.containsString(B::class.java.canonicalName))))

    registry.requireService(B::class.java)
  }

  @Test
  fun testInitCircularOptional() {
    val registry = this.serviceDirectory()

    registry.registerService(A::class.java) {
      this.logger.debug("construct B")
      registry.optionalService(B::class.java, caller = A::class.java)
      A()
    }

    registry.registerService(B::class.java) {
      this.logger.debug("construct B")
      registry.optionalService(A::class.java, caller = B::class.java)
      B()
    }

    this.expectedException.expect(ServiceConfigurationException::class.java)
    this.expectedException.expectMessage(
      AllOf(mutableListOf(
        StringContains.containsString(A::class.java.canonicalName),
        StringContains.containsString(B::class.java.canonicalName))))

    registry.optionalService(B::class.java)
  }
}
