package de.matrixweb.osgi.jetty.test5.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.api.ServletContext;

/**
 * @author markusw
 */
public class Activator implements BundleActivator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

  private final List<ServiceRegistration<?>> regs = new ArrayList<ServiceRegistration<?>>();

  @Override
  public void start(final BundleContext context) throws Exception {
    final ServletContextListener scl = new ServletContextListener() {
      @Override
      public void contextInitialized(final ServletContextEvent sce) {
        LOGGER.info("CONTEXT CREATED");
      }

      @Override
      public void contextDestroyed(final ServletContextEvent sce) {
        LOGGER.info("CONTEXT DESTROYED");
      }
    };
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    this.regs.add(context.registerService(EventListener.class, scl, props));
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    for (final ServiceRegistration<?> reg : this.regs) {
      reg.unregister();
    }
    this.regs.clear();
  }

}
