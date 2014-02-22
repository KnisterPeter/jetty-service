package de.matrixweb.osgi.jetty.server.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.Holder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.api.ServletContext;

public class Registrator {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(Registrator.class);

	private Jetty jetty;

	private Map<ServiceReference<Filter>, Filter> filters = new TreeMap<ServiceReference<Filter>, Filter>();

	private Map<ServiceReference<Servlet>, Servlet> servlets = new HashMap<ServiceReference<Servlet>, Servlet>();

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
		ServiceReference<ServletContext> reference = jetty
				.getServletContextReference(servletContext);
		for (Entry<ServiceReference<Filter>, Filter> filterPair : filters
				.entrySet()) {
			if (isMatchingContext(reference, filterPair.getKey())) {
				registerFilter(handler, filterPair.getKey(),
						filterPair.getValue());
			}
		}
		for (Entry<ServiceReference<Servlet>, Servlet> servletPair : servlets
				.entrySet()) {
			if (isMatchingContext(reference, servletPair.getKey())) {
				registerServlet(handler, servletPair.getKey(),
						servletPair.getValue());
			}
		}
	}

	public synchronized void removeServletContext(
			ServletContext servletContext, ServletContextHandler handler) {
		unregisterServletContext(servletContext, handler);
	}

	private void unregisterServletContext(ServletContext servletContext,
			ServletContextHandler handler) {
		ServiceReference<ServletContext> reference = jetty
				.getServletContextReference(servletContext);
		for (Entry<ServiceReference<Filter>, Filter> filterPair : filters
				.entrySet()) {
			if (isMatchingContext(reference, filterPair.getKey())) {
				unregisterFilter(handler, filterPair.getKey(),
						filterPair.getValue());
			}
		}
		for (Entry<ServiceReference<Servlet>, Servlet> servletPair : servlets
				.entrySet()) {
			if (isMatchingContext(reference, servletPair.getKey())) {
				unregisterServlet(handler, servletPair.getKey(),
						servletPair.getValue());
			}
		}
	}

	public synchronized void addFilter(ServiceReference<Filter> reference,
			Filter filter) {
		if (!filters.containsKey(reference)) {
			LOGGER.info("Adding filter: {}", filter);
			filters.put(reference, filter);
			registerFilter(reference, filter);
		}
	}

	private void registerFilter(ServiceReference<Filter> reference,
			Filter filter) {
		for (Entry<ServletContext, ServletContextHandler> pair : jetty
				.getHandlers().entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(pair.getKey()), reference)) {
				registerFilter(pair.getValue(), reference, filter);
			}
		}
	}

	public synchronized void removeFilter(ServiceReference<Filter> reference,
			Filter filter) {
		if (filters.containsKey(reference)) {
			LOGGER.info("Removing filter: {}", filter);
			filters.remove(reference);
			unregisterFilter(reference, filter);
		}
	}

	private void unregisterFilter(ServiceReference<Filter> reference,
			Filter filter) {
		for (Entry<ServletContext, ServletContextHandler> pair : jetty
				.getHandlers().entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(pair.getKey()), reference)) {
				unregisterFilter(pair.getValue(), reference, filter);
			}
		}
	}

	public synchronized void addServlet(ServiceReference<Servlet> reference,
			Servlet servlet) {
		if (!servlets.containsKey(reference)) {
			LOGGER.info("Adding servlet: {}", servlet);
			servlets.put(reference, servlet);
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
		if (servlets.containsKey(reference)) {
			LOGGER.info("Removing servlet: {}", servlet);
			servlets.remove(reference);
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

	/**
	 * Reregister all filters to insert new filters by specifed
	 * {@link Constants#SERVICE_RANKING} order.
	 */
	private void registerFilter(ServletContextHandler handler,
			ServiceReference<Filter> reference, Filter filter) {
		List<FilterHolder> holders = new ArrayList<FilterHolder>();
		List<FilterMapping> mapping = new ArrayList<FilterMapping>();
		for (Entry<FilterHolder, FilterMapping> entry : createFilters()
				.entrySet()) {
			holders.add(entry.getKey());
			mapping.add(entry.getValue());
		}
		Collections.reverse(holders);
		Collections.reverse(mapping);

		handler.getServletHandler().setFilters(
				holders.toArray(new FilterHolder[0]));
		handler.getServletHandler().setFilterMappings(
				mapping.toArray(new FilterMapping[0]));
	}

	private Map<FilterHolder, FilterMapping> createFilters() {
		Map<FilterHolder, FilterMapping> map = new LinkedHashMap<FilterHolder, FilterMapping>();
		for (Entry<ServiceReference<Filter>, Filter> entry : filters.entrySet()) {
			ServiceReference<Filter> reference = entry.getKey();
			Filter filter = entry.getValue();

			FilterHolder holder = new FilterHolder(filter);
			setName(holder, reference);
			holder.setAsyncSupported(true);
			for (String key : reference.getPropertyKeys()) {
				if (key.startsWith("init.")) {
					holder.setInitParameter(key.substring(5), reference
							.getProperty(key).toString());
				}
			}
			FilterMapping filterMapping = new FilterMapping();
			filterMapping.setFilterName(holder.getName());
			filterMapping.setDispatches(FilterMapping.ALL);
			Object mapping = reference.getProperty(ServletContext.ALIAS);
			if (mapping != null) {
				LOGGER.info("Mapping Filter to: {}", mapping);
				filterMapping.setPathSpec(mapping.toString());
			}
			Object servletName = reference.getProperty("servlet.name");
			if (servletName != null) {
				LOGGER.info("Mapping Filter to: {}", servletName);
				filterMapping.setServletName(servletName.toString());
			}

			map.put(holder, filterMapping);
		}
		return map;
	}

	private void unregisterFilter(ServletContextHandler handler,
			ServiceReference<Filter> reference, Filter filter) {
		List<String> names = new ArrayList<String>();
		List<FilterHolder> filters = new ArrayList<FilterHolder>();
		for (FilterHolder holder : handler.getServletHandler().getFilters()) {
			if (holder.getFilter().equals(filters)) {
				names.add(holder.getName());
			} else {
				filters.add(holder);
			}
		}

		List<FilterMapping> mappings = new ArrayList<FilterMapping>();
		if (handler.getServletHandler().getFilterMappings() != null) {
			for (FilterMapping mapping : handler.getServletHandler()
					.getFilterMappings()) {
				if (!names.contains(mapping.getFilterName())) {
					mappings.add(mapping);
				}
			}
		}

		handler.getServletHandler().setFilterMappings(
				mappings.toArray(new FilterMapping[0]));
		handler.getServletHandler().setFilters(
				filters.toArray(new FilterHolder[0]));
	}

	private void registerServlet(ServletContextHandler servletContextHandler,
			ServiceReference<Servlet> reference, Servlet servlet) {
		Object mapping = reference.getProperty(ServletContext.ALIAS);
		if (mapping != null) {
			LOGGER.info("Mapping Servlet to: {}", mapping);
			ServletHolder holder = new ServletHolder(servlet);
			setName(holder, reference);
			holder.setAsyncSupported(true);
			for (String key : reference.getPropertyKeys()) {
				if (key.startsWith("init.")) {
					holder.setInitParameter(key.substring(5), reference
							.getProperty(key).toString());
				}
			}
			servletContextHandler.addServlet(holder, mapping.toString());
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
				LOGGER.error("Failed to unregister servlet", e);
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
	}

	private boolean isMatchingContext(ServiceReference<?> servletContext,
			ServiceReference<?> servlet) {
		return servletContext.getProperty(ServletContext.CONTEXT_ID).equals(
				servlet.getProperty(ServletContext.CONTEXT_ID));
	}

	private void setName(Holder<?> holder, ServiceReference<?> reference) {
		Object name = reference.getProperty(ServletContext.NAME);
		if (name != null) {
			holder.setName(name.toString());
		}
	}

}
