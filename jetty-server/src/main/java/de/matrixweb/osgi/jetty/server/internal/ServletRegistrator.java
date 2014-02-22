package de.matrixweb.osgi.jetty.server.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.api.ServletContext;

public class ServletRegistrator {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ServletRegistrator.class);

	private Jetty jetty;

	private Map<Servlet, ServiceReference<Servlet>> servlets = new HashMap<Servlet, ServiceReference<Servlet>>();

	public void setJetty(Jetty jetty) {
		this.jetty = jetty;
	}

	public synchronized void refresh() {
		for (Entry<ServletContext, ServletContextHandler> pair : jetty
				.getHandlers().entrySet()) {
			registerServletContext(pair.getKey(), pair.getValue());
		}
	}

	public synchronized void addServletContext(ServletContext servletContext,
			ServletContextHandler handler) {
		registerServletContext(servletContext, handler);
	}

	private void registerServletContext(ServletContext servletContext,
			ServletContextHandler handler) {
		for (Entry<Servlet, ServiceReference<Servlet>> servletPair : servlets
				.entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(servletContext),
					servletPair.getValue())) {
				registerServlet(handler, servletPair.getValue(),
						servletPair.getKey());
			}
		}
	}

	public synchronized void removeServletContext(
			ServletContext servletContext, ServletContextHandler handler) {
		unregisterServletContext(servletContext, handler);
	}

	private void unregisterServletContext(ServletContext servletContext,
			ServletContextHandler handler) {
		for (Entry<Servlet, ServiceReference<Servlet>> servletPair : servlets
				.entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(servletContext),
					servletPair.getValue())) {
				unregisterServlet(handler, servletPair.getValue(),
						servletPair.getKey());
			}
		}
	}

	public synchronized void addServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		if (!servlets.containsKey(servlet)) {
			LOGGER.info("Adding servlet: {}", servlet);
			servlets.put(servlet, reference);
			registerServlet(reference, servlet);
		}
	}

	private void registerServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		for (Entry<ServletContext, ServletContextHandler> pair : jetty
				.getHandlers().entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(pair.getKey()), reference)) {
				registerServlet(pair.getValue(), reference, servlet);
			}
		}
	}

	public synchronized void removeServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		if (servlets.containsKey(servlet)) {
			LOGGER.info("Removing servlet: {}", servlet);
			servlets.remove(servlet);
			unregisterServlet(reference, servlet);
		}
	}

	private void unregisterServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		for (Entry<ServletContext, ServletContextHandler> pair : jetty
				.getHandlers().entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(pair.getKey()), reference)) {
				unregisterServlet(pair.getValue(), reference, servlet);
			}
		}
	}

	private void registerServlet(ServletContextHandler servletContextHandler,
			ServiceReference<Servlet> reference, Servlet servlet) {
		Object mapping = reference.getProperty("alias");
		if (mapping != null) {
			LOGGER.info("Mapping Servlet to: {}", mapping);
			ServletHolder holder = new ServletHolder(servlet);
			holder.setAsyncSupported(true);
			for (String key : reference.getPropertyKeys()) {
				if (key.startsWith("init.")) {
					holder.setInitParameter(key.substring(5), reference
							.getProperty(key).toString());
				}
			}
			servletContextHandler.getServletHandler().addServletWithMapping(
					holder, mapping.toString());
		} else {
			LOGGER.warn("Servlet without alias mapping skipped: {}", servlet);
		}
	}

	private void unregisterServlet(ServletContextHandler handler,
			ServiceReference<Servlet> reference, Servlet servlet) {
		List<String> names = new ArrayList<String>();
		List<ServletHolder> servlets = new ArrayList<ServletHolder>();
		for (ServletHolder holder : handler.getServletHandler().getServlets()) {
			try {
				if (holder.getServlet().equals(servlet)) {
					names.add(holder.getName());
				} else {
					servlets.add(holder);
				}
			} catch (ServletException e) {
				e.printStackTrace();
			}
		}

		List<ServletMapping> mappings = new ArrayList<ServletMapping>();

		if (handler.getServletHandler().getServletMappings() != null) {
			for (ServletMapping mapping : handler.getServletHandler()
					.getServletMappings()) {
				if (!names.contains(mapping.getServletName())) {
					mappings.add(mapping);
				}
			}
		}

		handler.getServletHandler().setServletMappings(
				mappings.toArray(new ServletMapping[0]));
		handler.getServletHandler().setServlets(
				servlets.toArray(new ServletHolder[0]));
		servlets.remove(servlet);
	}

	private boolean isMatchingContext(
			ServiceReference<ServletContext> servletContext,
			ServiceReference<Servlet> servlet) {
		return servletContext.getProperty("contextId").equals(
				servlet.getProperty("contextId"));
	}

}
