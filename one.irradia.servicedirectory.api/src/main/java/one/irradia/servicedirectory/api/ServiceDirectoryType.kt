package one.irradia.servicedirectory.api

/**
 * The service directory interface.
 */

interface ServiceDirectoryType {

  /**
   * Retrieve a mandatory reference to the service implementing the given class.
   *
   * @throws ServiceConfigurationException If no service is available implementing the given class, or if a circular dependency is detected
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> requireService(
    serviceClass: Class<T>,
    caller: Class<*>? = null): T

  /**
   * Retrieve an optional reference to the service implementing the given class. If no service
   * is available, the function returns `null`.
   *
   * @throws ServiceConfigurationException If a circular dependency is detected
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> optionalService(
    serviceClass: Class<T>,
    caller: Class<*>? = null): T?

  /**
   * Register a service. The given function must return a value of the given service type when
   * evaluated. The given function must expect to be called from any thread.
   *
   * @throws ServiceConfigurationException If a circular dependency is detected
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> registerService(
    serviceClass: Class<T>,
    service: () -> T)

}
