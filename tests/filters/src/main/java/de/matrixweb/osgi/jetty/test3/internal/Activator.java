package de.matrixweb.osgi.jetty.test3.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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
    createFilters(context);
  }

  private void createFilters(final BundleContext context) {
    final Filter filter = new Filter() {
      @Override
      public void init(final FilterConfig filterConfig) throws ServletException {
        LOGGER.info("init() FILTER#2 (mapped to *)");
      }

      @Override
      public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
        LOGGER.info("doFilter() FILTER#2 (mapped to *)");
        chain.doFilter(request, response);
      }

      @Override
      public void destroy() {
        LOGGER.info("destroy() FILTER#2 (mapped to *)");
      }
    };
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    props.put(ServletContext.ALIAS, "*");
    props.put(Constants.SERVICE_RANKING, 100);
    this.regs.add(context.registerService(Filter.class, filter, props));
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    for (final ServiceRegistration<?> reg : this.regs) {
      reg.unregister();
    }
    this.regs.clear();
  }

}
