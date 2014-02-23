package de.matrixweb.osgi.jetty.test4.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.api.ServletContext;

/**
 * @author markusw
 */
public class IndexActivator implements BundleActivator {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexActivator.class);

  private final List<ServiceRegistration<?>> regs = new ArrayList<ServiceRegistration<?>>();

  @Override
  public void start(final BundleContext context) throws Exception {
    createFilters(context);
    createServlets(context);
  }

  private void createFilters(final BundleContext context) {
    final Filter filter = new Filter() {
      @Override
      public void init(final FilterConfig filterConfig) throws ServletException {
        LOGGER.info("init() FILTER#Home");
      }

      @Override
      public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
        LOGGER.info("doFilter() FILTER#Home");
        chain.doFilter(request, response);
      }

      @Override
      public void destroy() {
        LOGGER.info("destroy() FILTER#Home");
      }
    };
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    props.put("servlet.name", "Home");
    this.regs.add(context.registerService(Filter.class, filter, props));
  }

  private void createServlets(final BundleContext context) {
    final Servlet servlet = new HttpServlet() {
      private static final long serialVersionUID = 316577294919021093L;

      @Override
      public void init(final ServletConfig config) throws ServletException {
        LOGGER.info("SERVLET init: {}", config);
        super.init(config);
      }

      @Override
      protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
          IOException {
        LOGGER.info("Requesting {}", req.getRequestURI());
        req.getSession().setAttribute("attr", "sess-value " + req.getSession().getId());

        resp.setContentType("text/html");
        final PrintWriter writer = resp.getWriter();
        writer.write("Hello World! (global=" + getServletContext().getInitParameter("name") + ")");
        writer.write("<br /><a href=\"/msg\">next</a>");
        writer.close();
      }

      @Override
      public void destroy() {
        LOGGER.info("SERVLET destroy");
        super.destroy();
      }
    };
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    props.put(ServletContext.ALIAS, "/index");
    props.put(ServletContext.NAME, "Home");
    this.regs.add(context.registerService(Servlet.class, servlet, props));
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    for (final ServiceRegistration<?> reg : this.regs) {
      reg.unregister();
    }
    this.regs.clear();
  }

}
