package de.matrixweb.osgi.jetty.test2.internal;

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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class ServletAndFilterActivator implements BundleActivator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServletAndFilterActivator.class);

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
        LOGGER.info("init() FILTER#1 (mapped to /msg)");
      }

      @Override
      public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
        LOGGER.info("doFilter() FILTER#1 (mapped to /msg)");
        chain.doFilter(request, response);
      }

      @Override
      public void destroy() {
        LOGGER.info("destroy() FILTER#1 (mapped to /msg)");
      }
    };
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    props.put(ServletContext.ALIAS, "/msg");
    props.put(Constants.SERVICE_RANKING, 1);
    this.regs.add(context.registerService(Filter.class, filter, props));
  }

  private void createServlets(final BundleContext context) {
    final Servlet servlet = new HttpServlet() {
      private static final long serialVersionUID = 2170992479025460048L;

      @Override
      protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
          IOException {
        LOGGER.info("Requesting {}", req.getRequestURI());
        final Object str = req.getSession().getAttribute("attr");
        req.getSession().removeAttribute("attr");

        resp.setContentType("text/html");
        final PrintWriter writer = resp.getWriter();
        writer.write("Hello World " + getInitParameter("number") + "!");
        writer.write("<br/>" + str);
        writer.write("<br /><a href=\"/index\">next</a>");
        writer.close();
      }
    };
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    props.put(ServletContext.ALIAS, "/msg");
    props.put("init.number", 5);
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
