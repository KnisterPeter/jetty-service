package de.matrixweb.osgi.jetty.test.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
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

import de.matrixweb.osgi.jetty.api.DefaultServletContext;
import de.matrixweb.osgi.jetty.api.ServletContext;

public class Activator implements BundleActivator {

	private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);
	
	private List<ServiceRegistration<?>> regs = new ArrayList<ServiceRegistration<?>>();
	
	@Override
	public void start(BundleContext context) throws Exception {
		// ServletContext
		DefaultServletContext servletContext = new DefaultServletContext();
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(ServletContext.CONTEXT_ID, "/");
		props.put("init.name", "value");
		regs.add(context.registerService(ServletContext.class, servletContext, props));
		
		ServletContextListener scl =new ServletContextListener() {
			@Override
			public void contextInitialized(ServletContextEvent sce) {
				LOGGER.info("CONTEXT CREATED");
			}
			
			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				LOGGER.info("CONTEXT DESTROYED");
			}
		};
		props.put(ServletContext.CONTEXT_ID, "/");
		regs.add(context.registerService(EventListener.class, scl, props));
		
		// Filter
		Filter filter = new Filter() {
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				LOGGER.info("Activator.start(...).new Filter() {...}.init() FILTER#1");
			}
			
			@Override
			public void doFilter(ServletRequest request, ServletResponse response,
					FilterChain chain) throws IOException, ServletException {
				LOGGER.info("Activator.start(...).new Filter() {...}.doFilter() FILTER#1");
				chain.doFilter(request, response);
			}
			
			@Override
			public void destroy() {
				LOGGER.info("Activator.start(...).new Filter() {...}.destroy() FILTER#1");
			}
		};
		props = new Hashtable<String, Object>();
		props.put(ServletContext.CONTEXT_ID, "/");
		props.put(ServletContext.ALIAS, "/msg");
		props.put(Constants.SERVICE_RANKING, 1);
		regs.add(context.registerService(Filter.class, filter, props));
		
		// Filter
		filter = new Filter() {
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				LOGGER.info("Activator.start(...).new Filter() {...}.init() FILTER#2");
			}
			
			@Override
			public void doFilter(ServletRequest request, ServletResponse response,
					FilterChain chain) throws IOException, ServletException {
				LOGGER.info("Activator.start(...).new Filter() {...}.doFilter() FILTER#2");
				chain.doFilter(request, response);
			}
			
			@Override
			public void destroy() {
				LOGGER.info("Activator.start(...).new Filter() {...}.destroy() FILTER#2");
			}
		};
		props = new Hashtable<String, Object>();
		props.put(ServletContext.CONTEXT_ID, "/");
		props.put(ServletContext.ALIAS, "*");
		props.put(Constants.SERVICE_RANKING, 100);
		regs.add(context.registerService(Filter.class, filter, props));
		
		// Filter
		filter = new Filter() {
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				LOGGER.info("Activator.start(...).new Filter() {...}.init() FILTER#Home");
			}
			
			@Override
			public void doFilter(ServletRequest request, ServletResponse response,
					FilterChain chain) throws IOException, ServletException {
				LOGGER.info("Activator.start(...).new Filter() {...}.doFilter() FILTER#Home");
				chain.doFilter(request, response);
			}
			
			@Override
			public void destroy() {
				LOGGER.info("Activator.start(...).new Filter() {...}.destroy() FILTER#Home");
			}
		};
		props = new Hashtable<String, Object>();
		props.put(ServletContext.CONTEXT_ID, "/");
		props.put("servlet.name", "Home");
		regs.add(context.registerService(Filter.class, filter, props));
		
		// Servlet
		Servlet servlet = new HttpServlet() {
			@Override
			public void init(ServletConfig config) throws ServletException {
				LOGGER.info("SERVLET init: {}", config);
				super.init(config);
			}
			@Override
			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
				req.getSession().setAttribute("attr", "sess-value " + req.getSession().getId());
				
				resp.setContentType("text/html");
				PrintWriter writer = resp.getWriter();
				writer.write("Hello World! (global="  + getServletContext().getInitParameter("name") + ")");
				writer.write("<br /><a href=\"/msg\">next</a>");
				writer.close();
			}
			
			@Override
			public void destroy() {
				LOGGER.info("SERVLET destroy");
				super.destroy();
			}
		};
		props = new Hashtable<String, Object>();
		props.put(ServletContext.CONTEXT_ID, "/");
		props.put(ServletContext.ALIAS, "/");
		props.put(ServletContext.NAME, "Home");
		regs.add(context.registerService(Servlet.class, servlet, props));

		// Servlet
		servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
				Object str = req.getSession().getAttribute("attr");
				req.getSession().removeAttribute("attr");
				
				resp.setContentType("text/html");
				PrintWriter writer = resp.getWriter();
				writer.write("Hello World " + getInitParameter("number") + "!");
				writer.write("<br/>" + str);
				writer.write("<br /><a href=\"/\">next</a>");
				writer.close();
			}
		};
		props = new Hashtable<String, Object>();
		props.put(ServletContext.CONTEXT_ID, "/");
		props.put(ServletContext.ALIAS, "/msg");
		props.put("init.number", 5);
		regs.add(context.registerService(Servlet.class, servlet, props));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		for (ServiceRegistration<?> reg : regs) {
			reg.unregister();
		}
		regs.clear();
	}

}
