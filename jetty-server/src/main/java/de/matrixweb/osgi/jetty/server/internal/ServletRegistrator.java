package de.matrixweb.osgi.jetty.server.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.context.ServletContext;

public class ServletRegistrator {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ServletRegistrator.class);

	private Set<Pair<ServletContext, ServletContextHandler>> servletContexts = Collections
			.synchronizedSet(new HashSet<Pair<ServletContext, ServletContextHandler>>());

	private Set<Pair<ServiceReference<Servlet>, Servlet>> servlets = Collections
			.synchronizedSet(new HashSet<Pair<ServiceReference<Servlet>, Servlet>>());

	public synchronized void addServletContext(ServletContext servletContext,
			ServletContextHandler handler) {
		Pair<ServletContext, ServletContextHandler> pair = new Pair<ServletContext, ServletContextHandler>(
				servletContext, handler);
		if (!servletContexts.contains(pair)) {
			LOGGER.info("Adding servlet context: {}", servletContext);
			servletContexts.add(pair);
			registerServletContext(servletContext, handler);
		}
	}

	private void registerServletContext(ServletContext servletContext,
			ServletContextHandler handler) {
		for (Pair<ServiceReference<Servlet>, Servlet> servletPair : servlets) {
			if (isMatchingContext(servletContext, servletPair.getKey())) {
				registerServlet(handler, servletPair.getKey(),
						servletPair.getValue());
			}
		}
	}

	public synchronized void removeServletContext(
			ServletContext servletContext, ServletContextHandler handler) {
		Pair<ServletContext, ServletContextHandler> pair = new Pair<ServletContext, ServletContextHandler>(
				servletContext, handler);
		if (servletContexts.contains(pair)) {
			LOGGER.info("Removing servlet context: {}", servletContext);
			servletContexts.remove(pair);
			unregisterServletContext(servletContext, handler);
		}
	}

	private void unregisterServletContext(ServletContext servletContext,
			ServletContextHandler handler) {
		for (Pair<ServiceReference<Servlet>, Servlet> servletPair : servlets) {
			if (isMatchingContext(servletContext, servletPair.getKey())) {
				unregisterServlet(handler, servletPair.getKey(),
						servletPair.getValue());
			}
		}
	}

	public synchronized void addServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		Pair<ServiceReference<Servlet>, Servlet> pair = new Pair<ServiceReference<Servlet>, Servlet>(
				reference, servlet);
		if (!servlets.contains(pair)) {
			LOGGER.info("Adding servlet: {}", servlet);
			servlets.add(pair);
			registerServlet(reference, servlet);
		}
	}

	private void registerServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		for (Pair<ServletContext, ServletContextHandler> pair : servletContexts) {
			if (isMatchingContext(pair.getKey(), reference)) {
				registerServlet(pair.getValue(), reference, servlet);
			}
		}
	}

	public synchronized void removeServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		Pair<ServiceReference<Servlet>, Servlet> pair = new Pair<ServiceReference<Servlet>, Servlet>(
				reference, servlet);
		if (servlets.contains(pair)) {
			LOGGER.info("Removing servlet: {}", servlet);
			servlets.remove(pair);
			unregisterServlet(reference, servlet);
		}
	}

	private void unregisterServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		for (Pair<ServletContext, ServletContextHandler> pair : servletContexts) {
			if (isMatchingContext(pair.getKey(), reference)) {
				unregisterServlet(pair.getValue(), reference, servlet);
			}
		}
	}

	private void registerServlet(ServletContextHandler servletContextHandler,
			ServiceReference<Servlet> reference, Servlet servlet) {
		// TODO: Init Params
		Object mapping = reference.getProperty("alias");
		if (mapping != null) {
			LOGGER.info("Mapping Servlet to: {}", mapping);
			servletContextHandler.getServletHandler().addServletWithMapping(
					new ServletHolder(servlet), mapping.toString());
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

	private boolean isMatchingContext(ServletContext servletContext,
			ServiceReference<Servlet> reference) {
		return servletContext.getContextPath().equals(
				reference.getProperty("contextId"));
	}

	private static class Pair<K, V> {

		private K key;

		private V value;

		public Pair(K reference, V servlet) {
			this.key = reference;
			this.value = servlet;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Pair<?, ?> other = (Pair<?, ?>) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

	}

}
