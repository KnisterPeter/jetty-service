package de.matrixweb.osgi.jetty.server.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import de.matrixweb.osgi.jetty.api.ServletContext;

public class ServletContextHandlerServiceTracker extends ServiceTracker<ServletContext, ServletContext> {

  private Jetty jetty;

  public ServletContextHandlerServiceTracker(final BundleContext context, final Jetty jetty) {
    super(context, ServletContext.class, null);
    this.jetty = jetty;
  }

  @Override
  public ServletContext addingService(final ServiceReference<ServletContext> reference) {
    final ServletContext servletContext = super.addingService(reference);
    this.jetty.addServletContext(servletContext, reference);
    return servletContext;
  }

  @Override
  public void removedService(final ServiceReference<ServletContext> reference, final ServletContext service) {
    this.jetty.removeServletContext(service, reference);
    super.removedService(reference, service);
  }

}
