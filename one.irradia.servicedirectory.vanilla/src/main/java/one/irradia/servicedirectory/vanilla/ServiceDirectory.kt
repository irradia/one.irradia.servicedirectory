package one.irradia.servicedirectory.vanilla

import com.google.common.collect.MapMaker
import one.irradia.servicedirectory.api.ServiceDirectoryType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

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

  private fun <T> loadServices(serviceClass: Class<T>): List<T> {
    this.logger.debug("loading services {} from ServiceLoader", serviceClass.canonicalName)
    return ServiceLoader.load(serviceClass).toList()
  }

  override fun <T : Any> optionalServices(
    serviceClass: Class<T>,
    caller: Class<*>?): List<T> {
    return if (!this.services.containsKey(serviceClass)) {
      val services = this.loadServices(serviceClass)
      this.services[serviceClass] =
        ServiceReference(serviceClass, init = services.map { service -> { service } })
      services
    } else {
      this.services[serviceClass]!!.get(caller) as List<T>
    }
  }

  override fun <T : Any> registerService(
    serviceClass: Class<T>,
    service: () -> T) {

    val reference = this.services[serviceClass] as ServiceReference<T>?
    if (reference == null) {
      this.services[serviceClass] = ServiceReference(serviceClass, init = listOf(service))
    } else {
      reference.addService(service)
    }

    this.logger.debug("registered service {}", serviceClass.canonicalName)
  }

  companion object {

    /**
     * Create a new service directory.
     */

    fun create(): ServiceDirectoryType =
      ServiceDirectory()

  }
}