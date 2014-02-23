package de.matrixweb.osgi.jetty.test1.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.matrixweb.osgi.jetty.api.DefaultServletContext;
import de.matrixweb.osgi.jetty.api.ServletContext;

/**
 * @author markusw
 */
public class Activator implements BundleActivator {

  private final List<ServiceRegistration<?>> regs = new ArrayList<ServiceRegistration<?>>();

  @Override
  public void start(final BundleContext context) throws Exception {
    final DefaultServletContext servletContext = new DefaultServletContext();
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    props.put("init.name", "value");
    this.regs.add(context.registerService(ServletContext.class, servletContext, props));
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    for (final ServiceRegistration<?> reg : this.regs) {
      reg.unregister();
    }
    this.regs.clear();
  }

}
