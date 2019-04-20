package one.irradia.servicedirectory.vanilla

import com.google.common.collect.MapMaker
import one.irradia.servicedirectory.api.ServiceConfigurationException
import one.irradia.servicedirectory.api.ServiceDirectoryType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A simple implementation of the [ServiceRegistryType] interface.
 */

class ServiceDirectory private constructor(): ServiceDirectoryType {

  private val logger =
    LoggerFactory.getLogger(ServiceDirectory::class.java)

  private val services =
    MapMaker()
      .concurrencyLevel(4)
      .initialCapacity(128)
      .makeMap<Class<*>, ServiceReference<*>>()

  override fun <T : Any> requireService(
    serviceClass: Class<T>,
    caller: Class<*>?): T =
    if (!this.services.containsKey(serviceClass)) {
      val service = this.loadRequiredService(serviceClass)
      this.services[serviceClass] = ServiceReference(serviceClass, init = { service })
      service
    } else {
      this.services[serviceClass]!!.get(caller) as T
    }

  override fun <T : Any> optionalService(
    serviceClass: Class<T>,
    caller: Class<*>?): T? =
    if (!this.services.containsKey(serviceClass)) {
      val service = this.loadOptionalService(serviceClass)
      if (service != null) {
        this.services[serviceClass] = ServiceReference(serviceClass, init = { service })
        service
      } else {
        null
      }
    } else {
      this.services[serviceClass]?.get(caller) as T?
    }

  private fun <T> loadRequiredService(serviceClass: Class<T>): T {
    this.logger.debug("loading required service {} from ServiceLoader", serviceClass.canonicalName)
    val loaded = ServiceLoader.load(serviceClass).toList()
    return if (loaded.isEmpty()) {
      throw ServiceConfigurationException(buildString {
        this.append("No service implementation is available\n")
        this.append("  Service: ${serviceClass.canonicalName}\n")
      })
    } else {
      loaded[0]
    }
  }

  private fun <T> loadOptionalService(serviceClass: Class<T>): T? {
    this.logger.debug("loading optional service {} from ServiceLoader", serviceClass.canonicalName)
    val loaded = ServiceLoader.load(serviceClass).toList()
    return if (loaded.isEmpty()) {
      null
    } else {
      loaded[0]
    }
  }

  override fun <T : Any> registerService(serviceClass: Class<T>, service: () -> T) {
    if (this.services.containsKey(serviceClass)) {
      throw ServiceConfigurationException(buildString {
        this.append("A service implementation is already registered\n")
        this.append("  Service: ${serviceClass.canonicalName}\n")
      })
    }

    this.services[serviceClass] = ServiceReference(serviceClass, init = service)
    this.logger.debug("registered service {}", serviceClass.canonicalName)
  }

  private class ServiceReference<T>(
    val serviceClass: Class<T>,
    val init: () -> T) {

    private val initializing = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    @Volatile
    private var reference: T? = null

    fun get(caller: Class<*>?) : T {
      return if (!this.initialized.get()) {
        if (this.initializing.compareAndSet(false, true)) {
          this.reference = this.init.invoke()
          this.initialized.set(true)
          this.reference!!
        } else {
          throw ServiceConfigurationException(buildString {
            this.append("Circular service dependency detected\n")
            this.append("  Service: ${this@ServiceReference.serviceClass.canonicalName}\n")
            if (caller != null) {
              this.append("  Caller:  ${caller.canonicalName}\n")
            }
          })
        }
      } else {
        this.reference!!
      }
    }
  }

  companion object {

    /**
     * Create a new service directory.
     */

    fun create(): ServiceDirectoryType =
      ServiceDirectory()

  }
}