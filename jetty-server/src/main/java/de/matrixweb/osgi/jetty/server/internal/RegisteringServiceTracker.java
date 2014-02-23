package de.matrixweb.osgi.jetty.server.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implements a service tracker exposing all services to the given registration
 * caller.
 * 
 * @param <T>
 *          The service interface type
 * 
 * @author markusw
 */
public class RegisteringServiceTracker<T> extends ServiceTracker<T, T> {

  private final RegistrationCaller<T> caller;

  /**
   * @param context
   *          The {@link BundleContext}
   * @param clazz
   *          The service interface
   * @param caller
   *          The {@link RegistrationCaller} to expose services to
   */
  public RegisteringServiceTracker(final BundleContext context, final Class<T> clazz, final RegistrationCaller<T> caller) {
    super(context, clazz, null);
    this.caller = caller;
  }

  @Override
  public T addingService(final ServiceReference<T> reference) {
    final T service = super.addingService(reference);
    if (service != null) {
      this.caller.add(reference, service);
    }
    return service;
  }

  @Override
  public void removedService(final ServiceReference<T> reference, final T service) {
    this.caller.remove(reference, service);
    super.removedService(reference, service);
  }

  /**
   * Callback interface for exposed services
   * 
   * @param <T>
   *          The service interface
   */
  public static interface RegistrationCaller<T> {

    /**
     * @param reference
     * @param service
     */
    void add(ServiceReference<T> reference, T service);

    /**
     * @param reference
     * @param service
     */
    void remove(ServiceReference<T> reference, T service);

  }

}
