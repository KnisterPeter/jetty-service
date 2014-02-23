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

/**
 * @author markusw
 */
public class Registrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Registrator.class);

  private Jetty jetty;

  private final Map<ServiceReference<EventListener>, EventListener> listeners = new HashMap<ServiceReference<EventListener>, EventListener>();

  private final Map<ServiceReference<Filter>, Filter> filters = new TreeMap<ServiceReference<Filter>, Filter>(
      new Comparator<ServiceReference<Filter>>() {
        @Override
        public int compare(final ServiceReference<Filter> r1, final ServiceReference<Filter> r2) {
          // Reverses the order of service-references to have the
          // priorized filter first
          return -r1.compareTo(r2);
        }
      });

  private final Map<ServiceReference<Servlet>, Servlet> servlets = new HashMap<ServiceReference<Servlet>, Servlet>();

  void setJetty(final Jetty jetty) {
    this.jetty = jetty;
  }

  synchronized void refresh() {
    for (final Entry<ServletContext, ServletContextHandler> pair : this.jetty.getHandlers().entrySet()) {
      registerServletContext(pair.getKey(), pair.getValue());
    }
  }

  /**
   * @param servletContext
   * @param handler
   */
  public synchronized void addServletContext(final ServletContext servletContext, final ServletContextHandler handler) {
    registerServletContext(servletContext, handler);
  }

  private void registerServletContext(final ServletContext servletContext, final ServletContextHandler handler) {
    final ServiceReference<ServletContext> reference = this.jetty.getServletContextReference(servletContext);
    for (final Entry<ServiceReference<EventListener>, EventListener> listenerPair : this.listeners.entrySet()) {
      if (isMatchingContext(reference, listenerPair.getKey())) {
        registerListener(handler, listenerPair.getKey(), listenerPair.getValue());
      }
    }
    for (final Entry<ServiceReference<Filter>, Filter> filterPair : this.filters.entrySet()) {
      if (isMatchingContext(reference, filterPair.getKey())) {
        registerFilter(handler, filterPair.getKey(), filterPair.getValue());
      }
    }
    for (final Entry<ServiceReference<Servlet>, Servlet> servletPair : this.servlets.entrySet()) {
      if (isMatchingContext(reference, servletPair.getKey())) {
        registerServlet(handler, servletPair.getKey(), servletPair.getValue());
      }
    }
  }

  /**
   * @param servletContext
   * @param handler
   */
  public synchronized void removeServletContext(final ServletContext servletContext, final ServletContextHandler handler) {
    unregisterServletContext(servletContext, handler);
  }

  private void unregisterServletContext(final ServletContext servletContext, final ServletContextHandler handler) {
    final ServiceReference<ServletContext> reference = this.jetty.getServletContextReference(servletContext);
    for (final Entry<ServiceReference<EventListener>, EventListener> listenerPair : this.listeners.entrySet()) {
      if (isMatchingContext(reference, listenerPair.getKey())) {
        unregisterListener(handler, listenerPair.getKey(), listenerPair.getValue());
      }
    }
    for (final Entry<ServiceReference<Filter>, Filter> filterPair : this.filters.entrySet()) {
      if (isMatchingContext(reference, filterPair.getKey())) {
        unregisterFilter(handler, filterPair.getKey(), filterPair.getValue());
      }
    }
    for (final Entry<ServiceReference<Servlet>, Servlet> servletPair : this.servlets.entrySet()) {
      if (isMatchingContext(reference, servletPair.getKey())) {
        unregisterServlet(handler, servletPair.getKey(), servletPair.getValue());
      }
    }
  }

  /**
   * @param reference
   * @param listener
   */
  public synchronized void addListener(final ServiceReference<EventListener> reference, final EventListener listener) {
    if (listener instanceof ServletContextListener || listener instanceof ServletContextAttributeListener
        || listener instanceof ServletRequestListener || listener instanceof ServletRequestAttributeListener) {
      if (!this.listeners.containsKey(reference)) {
        this.listeners.put(reference, listener);
        registerListener(reference, listener);
      }
    }
  }

  private void registerListener(final ServiceReference<EventListener> reference, final EventListener listener) {
    for (final Entry<ServletContext, ServletContextHandler> pair : this.jetty.getHandlers().entrySet()) {
      if (isMatchingContext(this.jetty.getServletContextReference(pair.getKey()), reference)) {
        registerListener(pair.getValue(), reference, listener);
      }
    }
  }

  /**
   * @param reference
   * @param listener
   */
  public synchronized void removeListener(final ServiceReference<EventListener> reference, final EventListener listener) {
    if (this.listeners.containsKey(reference)) {
      this.listeners.remove(reference);
      unregisterListener(reference, listener);
    }
  }

  private void unregisterListener(final ServiceReference<EventListener> reference, final EventListener listener) {
    for (final Entry<ServletContext, ServletContextHandler> pair : this.jetty.getHandlers().entrySet()) {
      if (isMatchingContext(this.jetty.getServletContextReference(pair.getKey()), reference)) {
        unregisterListener(pair.getValue(), reference, listener);
      }
    }
  }

  /**
   * @param reference
   * @param filter
   */
  public synchronized void addFilter(final ServiceReference<Filter> reference, final Filter filter) {
    if (!this.filters.containsKey(reference)) {
      this.filters.put(reference, filter);
      registerFilter(reference, filter);
    }
  }

  private void registerFilter(final ServiceReference<Filter> reference, final Filter filter) {
    for (final Entry<ServletContext, ServletContextHandler> pair : this.jetty.getHandlers().entrySet()) {
      if (isMatchingContext(this.jetty.getServletContextReference(pair.getKey()), reference)) {
        registerFilter(pair.getValue(), reference, filter);
      }
    }
  }

  /**
   * @param reference
   * @param filter
   */
  public synchronized void removeFilter(final ServiceReference<Filter> reference, final Filter filter) {
    if (this.filters.containsKey(reference)) {
      this.filters.remove(reference);
      unregisterFilter(reference, filter);
    }
  }

  private void unregisterFilter(final ServiceReference<Filter> reference, final Filter filter) {
    for (final Entry<ServletContext, ServletContextHandler> pair : this.jetty.getHandlers().entrySet()) {
      if (isMatchingContext(this.jetty.getServletContextReference(pair.getKey()), reference)) {
        unregisterFilter(pair.getValue(), reference, filter);
      }
    }
  }

  /**
   * @param reference
   * @param servlet
   */
  public synchronized void addServlet(final ServiceReference<Servlet> reference, final Servlet servlet) {
    if (!this.servlets.containsKey(reference)) {
      this.servlets.put(reference, servlet);
      registerServlet(reference, servlet);
    }
  }

  private void registerServlet(final ServiceReference<Servlet> reference, final Servlet servlet) {
    for (final Entry<ServletContext, ServletContextHandler> pair : this.jetty.getHandlers().entrySet()) {
      if (isMatchingContext(this.jetty.getServletContextReference(pair.getKey()), reference)) {
        registerServlet(pair.getValue(), reference, servlet);
      }
    }
  }

  /**
   * @param reference
   * @param servlet
   */
  public synchronized void removeServlet(final ServiceReference<Servlet> reference, final Servlet servlet) {
    if (this.servlets.containsKey(reference)) {
      this.servlets.remove(reference);
      unregisterServlet(reference, servlet);
    }
  }

  private void unregisterServlet(final ServiceReference<Servlet> reference, final Servlet servlet) {
    for (final Entry<ServletContext, ServletContextHandler> pair : this.jetty.getHandlers().entrySet()) {
      if (isMatchingContext(this.jetty.getServletContextReference(pair.getKey()), reference)) {
        unregisterServlet(pair.getValue(), reference, servlet);
      }
    }
  }

  private void registerListener(final ServletContextHandler handler, final ServiceReference<EventListener> reference,
      final EventListener listener) {
    LOGGER.info("Adding listener: {}", listener);

    handler.addEventListener(listener);
    if (listener instanceof ServletContextListener) {
      ((ServletContextListener) listener).contextInitialized(new ServletContextEvent(handler.getServletContext()));
    }
  }

  private void unregisterListener(final ServletContextHandler handler, final ServiceReference<EventListener> reference,
      final EventListener listener) {
    LOGGER.info("Removing listener: {}", listener);

    if (handler.getEventListeners() != null) {
      final List<EventListener> listeners = new ArrayList<EventListener>();
      for (final EventListener el : handler.getEventListeners()) {
        if (el == listener) {
          if (listener instanceof ServletContextListener) {
            ((ServletContextListener) listener).contextDestroyed(new ServletContextEvent(handler.getServletContext()));
          }
        } else {
          listeners.add(el);
        }
      }
      handler.setEventListeners(listeners.toArray(new EventListener[0]));
    }
  }

  private void registerFilter(final ServletContextHandler handler, final ServiceReference<Filter> reference,
      final Filter filter) {
    LOGGER.info("Adding filter: {}", filter);

    final List<FilterHolder> holders = new ArrayList<FilterHolder>(Arrays.asList(handler.getServletHandler()
        .getFilters()));
    final List<FilterMapping> mappings = new ArrayList<FilterMapping>();
    if (holders.size() > 0) {
      mappings.addAll(Arrays.asList(handler.getServletHandler().getFilterMappings()));
    }

    final int idx = new ArrayList<ServiceReference<Filter>>(this.filters.keySet()).indexOf(reference);

    final FilterHolder newHolder = new FilterHolder(filter);
    setName(newHolder, reference);
    newHolder.setAsyncSupported(true);
    for (final String key : reference.getPropertyKeys()) {
      if (key.startsWith("init.")) {
        newHolder.setInitParameter(key.substring(5), reference.getProperty(key).toString());
      }
    }
    if (holders.size() <= idx) {
      holders.add(newHolder);
    } else {
      holders.add(idx, newHolder);
    }

    final FilterMapping filterMapping = new FilterMapping();
    filterMapping.setFilterName(newHolder.getName());
    filterMapping.setDispatches(FilterMapping.ALL);
    final Object alias = reference.getProperty(ServletContext.ALIAS);
    if (alias != null) {
      LOGGER.info("Mapping Filter to: {}", alias);
      filterMapping.setPathSpec(alias.toString());
    }
    final Object servletName = reference.getProperty("servlet.name");
    if (servletName != null) {
      LOGGER.info("Mapping Filter to: {}", servletName);
      filterMapping.setServletName(servletName.toString());
    }
    if (mappings.size() <= idx) {
      mappings.add(filterMapping);
    } else {
      mappings.add(idx, filterMapping);
    }

    handler.getServletHandler().setFilters(holders.toArray(new FilterHolder[0]));
    handler.getServletHandler().setFilterMappings(mappings.toArray(new FilterMapping[0]));
  }

  private void unregisterFilter(final ServletContextHandler handler, final ServiceReference<Filter> reference,
      final Filter filter) {
    LOGGER.info("Removing filter: {}", filter);

    final List<String> names = new ArrayList<String>();
    final List<FilterHolder> filters = new ArrayList<FilterHolder>();
    final List<FilterMapping> mappings = new ArrayList<FilterMapping>();
    for (final FilterHolder holder : handler.getServletHandler().getFilters()) {
      if (holder.getFilter() == filter) {
        names.add(holder.getName());
      } else {
        filters.add(holder);
      }
    }
    if (handler.getServletHandler().getFilterMappings() != null) {
      for (final FilterMapping mapping : handler.getServletHandler().getFilterMappings()) {
        if (!names.contains(mapping.getFilterName())) {
          mappings.add(mapping);
        }
      }
    }

    handler.getServletHandler().setFilterMappings(mappings.toArray(new FilterMapping[0]));
    handler.getServletHandler().setFilters(filters.toArray(new FilterHolder[0]));

    filter.destroy();
  }

  private void registerServlet(final ServletContextHandler servletContextHandler,
      final ServiceReference<Servlet> reference, final Servlet servlet) {
    LOGGER.info("Adding servlet: {}", servlet);

    final Object mapping = reference.getProperty(ServletContext.ALIAS);
    if (mapping != null) {
      LOGGER.info("Mapping Servlet to: {}", mapping);
      final ServletHolder holder = new ServletHolder(servlet);
      setName(holder, reference);
      holder.setAsyncSupported(true);
      for (final String key : reference.getPropertyKeys()) {
        if (key.startsWith("init.")) {
          holder.setInitParameter(key.substring(5), reference.getProperty(key).toString());
        }
      }
      servletContextHandler.addServlet(holder, mapping.toString());
    } else {
      LOGGER.warn("Servlet without alias mapping skipped: {}", servlet);
    }
  }

  private void unregisterServlet(final ServletContextHandler handler, final ServiceReference<Servlet> reference,
      final Servlet servlet) {
    LOGGER.info("Removing servlet: {}", servlet);

    final List<String> names = new ArrayList<String>();
    final List<ServletHolder> servlets = new ArrayList<ServletHolder>();
    for (final ServletHolder holder : handler.getServletHandler().getServlets()) {
      try {
        if (holder.getServlet() == servlet) {
          names.add(holder.getName());
        } else {
          servlets.add(holder);
        }
      } catch (final ServletException e) {
        LOGGER.error("Failed to unregister servlet", e);
      }
    }

    final List<ServletMapping> mappings = new ArrayList<ServletMapping>();
    if (handler.getServletHandler().getServletMappings() != null) {
      for (final ServletMapping mapping : handler.getServletHandler().getServletMappings()) {
        if (!names.contains(mapping.getServletName())) {
          mappings.add(mapping);
        }
      }
    }

    handler.getServletHandler().setServletMappings(mappings.toArray(new ServletMapping[0]));
    handler.getServletHandler().setServlets(servlets.toArray(new ServletHolder[0]));

    servlet.destroy();
  }

  private boolean isMatchingContext(final ServiceReference<?> servletContext, final ServiceReference<?> servlet) {
    return servletContext.getProperty(ServletContext.CONTEXT_ID).equals(servlet.getProperty(ServletContext.CONTEXT_ID));
  }

  private void setName(final Holder<?> holder, final ServiceReference<?> reference) {
    final Object name = reference.getProperty(ServletContext.NAME);
    if (name != null) {
      holder.setName(name.toString());
    }
  }

}
