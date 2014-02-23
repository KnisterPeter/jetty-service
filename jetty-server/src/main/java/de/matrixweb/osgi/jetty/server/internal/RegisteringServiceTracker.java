package de.matrixweb.osgi.jetty.server.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class RegisteringServiceTracker<T> extends ServiceTracker<T, T> {

  private RegistrationCaller<T> caller;

  public RegisteringServiceTracker(final BundleContext context, final Class<T> clazz, final RegistrationCaller<T> caller) {
    super(context, clazz, null);
    this.caller = caller;
  }

  @Override
  public T addingService(final ServiceReference<T> reference) {
    final T service = super.addingService(reference);
    this.caller.add(reference, service);
    return service;
  }

  @Override
  public void removedService(final ServiceReference<T> reference, final T service) {
    this.caller.remove(reference, service);
    super.removedService(reference, service);
  }

  public static interface RegistrationCaller<T> {

    void add(ServiceReference<T> reference, T service);

    void remove(ServiceReference<T> reference, T service);

  }

}
