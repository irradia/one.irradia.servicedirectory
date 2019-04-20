package one.irradia.servicedirectory.vanilla

import one.irradia.servicedirectory.api.ServiceConfigurationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class ServiceReference<T>(
  private val serviceClass: Class<T>,
  init: List<() -> T>) {

  private val initializers = AtomicReference<List<() -> T>>(init)
  @Volatile private var reference: List<T>? = null

  private val initializing = AtomicBoolean(false)
  private val initialized = AtomicBoolean(false)

  internal fun get(caller: Class<*>?) : List<T> {
    return if (!this.initialized.get()) {
      if (this.initializing.compareAndSet(false, true)) {
        try {
          this.reference = this.initializers.get()!!.map { initializer -> initializer.invoke() }
          this.reference!!
        } finally {
          this.initializers.set(listOf())
          this.initialized.set(true)
          this.initializing.set(false)
        }
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

  internal fun addService(service: () -> T) {
    this.initializers.set(this.initializers.get().plus(service))
    this.initialized.set(false)
  }
}