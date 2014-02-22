package de.matrixweb.osgi.jetty.server.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.Holder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.api.ServletContext;

public class Registrator {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(Registrator.class);

	private Jetty jetty;

	private Map<ServiceReference<EventListener>, EventListener> listeners = new HashMap<ServiceReference<EventListener>, EventListener>();

	private Map<ServiceReference<Filter>, Filter> filters = new TreeMap<ServiceReference<Filter>, Filter>(
			new Comparator<ServiceReference<Filter>>() {
				@Override
				public int compare(ServiceReference<Filter> r1,
						ServiceReference<Filter> r2) {
					// Reverses the order of service-references to have the
					// priorized filter first
					return -r1.compareTo(r2);
				}
			});

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
		for (Entry<ServiceReference<EventListener>, EventListener> listenerPair : listeners
				.entrySet()) {
			if (isMatchingContext(reference, listenerPair.getKey())) {
				registerListener(handler, listenerPair.getKey(),
						listenerPair.getValue());
			}
		}
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
		for (Entry<ServiceReference<EventListener>, EventListener> listenerPair : listeners
				.entrySet()) {
			if (isMatchingContext(reference, listenerPair.getKey())) {
				unregisterListener(handler, listenerPair.getKey(),
						listenerPair.getValue());
			}
		}
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

	public synchronized void addListener(
			ServiceReference<EventListener> reference, EventListener listener) {
		if (listener instanceof ServletContextListener
				|| listener instanceof ServletContextAttributeListener
				|| listener instanceof ServletRequestListener
				|| listener instanceof ServletRequestAttributeListener) {
			if (!listeners.containsKey(reference)) {
				listeners.put(reference, listener);
				registerListener(reference, listener);
			}
		}
	}

	private void registerListener(ServiceReference<EventListener> reference,
			EventListener listener) {
		for (Entry<ServletContext, ServletContextHandler> pair : jetty
				.getHandlers().entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(pair.getKey()), reference)) {
				registerListener(pair.getValue(), reference, listener);
			}
		}
	}

	public synchronized void removeListener(
			ServiceReference<EventListener> reference, EventListener listener) {
		if (listeners.containsKey(reference)) {
			listeners.remove(reference);
			unregisterListener(reference, listener);
		}
	}

	private void unregisterListener(ServiceReference<EventListener> reference,
			EventListener listener) {
		for (Entry<ServletContext, ServletContextHandler> pair : jetty
				.getHandlers().entrySet()) {
			if (isMatchingContext(
					jetty.getServletContextReference(pair.getKey()), reference)) {
				unregisterListener(pair.getValue(), reference, listener);
			}
		}
	}

	public synchronized void addFilter(ServiceReference<Filter> reference,
			Filter filter) {
		if (!filters.containsKey(reference)) {
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

	private void registerListener(ServletContextHandler handler,
			ServiceReference<EventListener> reference, EventListener listener) {
		LOGGER.info("Adding listener: {}", listener);

		handler.addEventListener(listener);
		if (listener instanceof ServletContextListener) {
			((ServletContextListener) listener)
					.contextInitialized(new ServletContextEvent(handler
							.getServletContext()));
		}
	}

	private void unregisterListener(ServletContextHandler handler,
			ServiceReference<EventListener> reference, EventListener listener) {
		LOGGER.info("Removing listener: {}", listener);

		if (handler.getEventListeners() != null) {
			List<EventListener> listeners = new ArrayList<EventListener>();
			for (EventListener el : handler.getEventListeners()) {
				if (el == listener) {
					if (listener instanceof ServletContextListener) {
						((ServletContextListener) listener)
								.contextDestroyed(new ServletContextEvent(
										handler.getServletContext()));
					}
				} else {
					listeners.add(el);
				}
			}
			handler.setEventListeners(listeners.toArray(new EventListener[0]));
		}
	}

	private void registerFilter(ServletContextHandler handler,
			ServiceReference<Filter> reference, Filter filter) {
		LOGGER.info("Adding filter: {}", filter);

		List<FilterHolder> holders = new ArrayList<FilterHolder>(
				Arrays.asList(handler.getServletHandler().getFilters()));
		List<FilterMapping> mappings = new ArrayList<FilterMapping>();
		if (holders.size() > 0) {
			mappings.addAll(Arrays.asList(handler.getServletHandler()
					.getFilterMappings()));
		}

		int idx = new ArrayList<ServiceReference<Filter>>(filters.keySet())
				.indexOf(reference);

		FilterHolder newHolder = new FilterHolder(filter);
		setName(newHolder, reference);
		newHolder.setAsyncSupported(true);
		for (String key : reference.getPropertyKeys()) {
			if (key.startsWith("init.")) {
				newHolder.setInitParameter(key.substring(5), reference
						.getProperty(key).toString());
			}
		}
		if (holders.size() <= idx) {
			holders.add(newHolder);
		} else {
			holders.add(idx, newHolder);
		}

		FilterMapping filterMapping = new FilterMapping();
		filterMapping.setFilterName(newHolder.getName());
		filterMapping.setDispatches(FilterMapping.ALL);
		Object alias = reference.getProperty(ServletContext.ALIAS);
		if (alias != null) {
			LOGGER.info("Mapping Filter to: {}", alias);
			filterMapping.setPathSpec(alias.toString());
		}
		Object servletName = reference.getProperty("servlet.name");
		if (servletName != null) {
			LOGGER.info("Mapping Filter to: {}", servletName);
			filterMapping.setServletName(servletName.toString());
		}
		if (mappings.size() <= idx) {
			mappings.add(filterMapping);
		} else {
			mappings.add(idx, filterMapping);
		}

		handler.getServletHandler().setFilters(
				holders.toArray(new FilterHolder[0]));
		handler.getServletHandler().setFilterMappings(
				mappings.toArray(new FilterMapping[0]));
	}

	private void unregisterFilter(ServletContextHandler handler,
			ServiceReference<Filter> reference, Filter filter) {
		LOGGER.info("Removing filter: {}", filter);

		List<String> names = new ArrayList<String>();
		List<FilterHolder> filters = new ArrayList<FilterHolder>();
		List<FilterMapping> mappings = new ArrayList<FilterMapping>();
		for (FilterHolder holder : handler.getServletHandler().getFilters()) {
			if (holder.getFilter() == filter) {
				names.add(holder.getName());
			} else {
				filters.add(holder);
			}
		}
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
		
		filter.destroy();
	}

	private void registerServlet(ServletContextHandler servletContextHandler,
			ServiceReference<Servlet> reference, Servlet servlet) {
		LOGGER.info("Adding servlet: {}", servlet);

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
		LOGGER.info("Removing servlet: {}", servlet);

		List<String> names = new ArrayList<String>();
		List<ServletHolder> servlets = new ArrayList<ServletHolder>();
		for (ServletHolder holder : handler.getServletHandler().getServlets()) {
			try {
				if (holder.getServlet() == servlet) {
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
		
		servlet.destroy();
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
