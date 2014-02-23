package de.matrixweb.osgi.jetty.server.internal;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.matrixweb.osgi.jetty.api.ServletContext;
import de.matrixweb.osgi.jetty.server.internal.RegisteringServiceTracker.RegistrationCaller;

/**
 * @author markusw
 */
public class Activator implements BundleActivator {

  private Jetty jetty;

  private Registrator registrator;

  private List<RegisteringServiceTracker<?>> trackers = new ArrayList<RegisteringServiceTracker<?>>();

  /**
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(final BundleContext context) throws Exception {
    this.registrator = new Registrator();
    this.jetty = new Jetty(context, this.registrator);

    createServletContextTracker(context);
    createListenerTracker(context);
    createFilterTracker(context);
    createServletTracker(context);

    for (final RegisteringServiceTracker<?> tracker : this.trackers) {
      tracker.open();
    }
  }

  private void createServletContextTracker(final BundleContext context) {
    this.trackers.add(new RegisteringServiceTracker<ServletContext>(context, ServletContext.class,
        new RegistrationCaller<ServletContext>() {
          @Override
          public void add(final ServiceReference<ServletContext> reference, final ServletContext service) {
            Activator.this.jetty.addServletContext(service, reference);
          }

          @Override
          public void remove(final ServiceReference<ServletContext> reference, final ServletContext service) {
            Activator.this.jetty.removeServletContext(service, reference);
          }
        }));
  }

  private void createListenerTracker(final BundleContext context) {
    this.trackers.add(new RegisteringServiceTracker<EventListener>(context, EventListener.class,
        new RegistrationCaller<EventListener>() {
          @Override
          public void add(final ServiceReference<EventListener> reference, final EventListener service) {
            Activator.this.registrator.addListener(reference, service);
          }

          @Override
          public void remove(final ServiceReference<EventListener> reference, final EventListener service) {
            Activator.this.registrator.removeListener(reference, service);
          }
        }));
  }

  private void createFilterTracker(final BundleContext context) {
    this.trackers.add(new RegisteringServiceTracker<Filter>(context, Filter.class, new RegistrationCaller<Filter>() {
      @Override
      public void add(final ServiceReference<Filter> reference, final Filter service) {
        Activator.this.registrator.addFilter(reference, service);
      }

      @Override
      public void remove(final ServiceReference<Filter> reference, final Filter service) {
        Activator.this.registrator.removeFilter(reference, service);
      }
    }));
  }

  private void createServletTracker(final BundleContext context) {
    this.trackers.add(new RegisteringServiceTracker<Servlet>(context, Servlet.class, new RegistrationCaller<Servlet>() {
      @Override
      public void add(final ServiceReference<Servlet> reference, final Servlet service) {
        Activator.this.registrator.addServlet(reference, service);
      }

      @Override
      public void remove(final ServiceReference<Servlet> reference, final Servlet service) {
        Activator.this.registrator.removeServlet(reference, service);
      }
    }));
  }

  /**
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(final BundleContext context) throws Exception {
    for (final RegisteringServiceTracker<?> tracker : this.trackers) {
      tracker.close();
    }
    this.trackers.clear();
    if (this.jetty != null) {
      this.jetty.dispose();
      this.jetty = null;
    }
  }

}
