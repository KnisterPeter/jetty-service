package de.matrixweb.osgi.jetty.server.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.api.ServletContext;

/**
 * @author marwol
 */
public class Jetty implements ManagedService {

  private static final Logger LOGGER = LoggerFactory.getLogger(Jetty.class);

  private ServiceRegistration<ManagedService> registration;

  private Server server;

  private ContextHandlerCollection handlerCollection;

  private final Registrator registrator;

  private final Map<ServletContext, ServiceReference<ServletContext>> servletContexts = Collections
      .synchronizedMap(new HashMap<ServletContext, ServiceReference<ServletContext>>());

  private final Map<ServletContext, ServletContextHandler> handlers = new HashMap<ServletContext, ServletContextHandler>();

  /**
   * @param context
   * @param registrator
   */
  public Jetty(final BundleContext context, final Registrator registrator) {
    this.registrator = registrator;
    registrator.setJetty(this);
    this.registration = context.registerService(ManagedService.class, this, getDefaultProperties());
  }

  private Dictionary<String, Object> getDefaultProperties() {
    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(Constants.SERVICE_PID, "jettyservice");
    return props;
  }

  /**
   * 
   */
  public void dispose() {
    if (this.registration != null) {
      try {
        updated(null);
      } catch (final ConfigurationException e) {
        LOGGER.error("Failed to stop jetty server", e);
      }
      this.registration.unregister();
      this.registration = null;
    }
  }

  @Override
  public synchronized void updated(final Dictionary<String, ?> properties) throws ConfigurationException {
    try {
      if (this.server != null) {
        this.server.stop();
        this.server = null;
        this.handlerCollection = null;
        this.handlers.clear();
      }
      if (properties != null) {
        this.server = new Server();

        final SelectChannelConnector connector = new SelectChannelConnector();
        connector.setName(getString(properties, "connector", null));
        connector.setHost(getString(properties, "host", null));
        connector.setPort(getInteger(properties, "port", 8181));
        this.server.addConnector(connector);
        this.handlerCollection = new ContextHandlerCollection();
        this.server.setHandler(this.handlerCollection);
        this.server.start();
        LOGGER.info("Started jetty server");

        for (final Entry<ServletContext, ServiceReference<ServletContext>> entry : this.servletContexts.entrySet()) {
          registerServletContext(entry.getKey(), entry.getValue());
        }
      } else {
        LOGGER.info("Stopped jetty server");
      }
    } catch (final Exception e) {
      throw new ConfigurationException("unknown", "unknown", e);
    }
    if (this.registration != null) {
      this.registration.setProperties(properties != null ? properties : getDefaultProperties());
    }
  }

  private String getString(final Dictionary<String, ?> properties, final String key, final String ifNull) {
    final Object value = properties.get(key);
    return value != null ? value.toString() : ifNull;
  }

  private Integer getInteger(final Dictionary<String, ?> properties, final String key, final Integer ifNull) {
    final Object value = properties.get(key);
    return value != null ? Integer.valueOf(value.toString()) : ifNull;
  }

  /**
   * @param servletContext
   * @param reference
   */
  public synchronized void addServletContext(final ServletContext servletContext,
      final ServiceReference<ServletContext> reference) {
    LOGGER.debug("Adding ServletContext: {}", servletContext);
    this.servletContexts.put(servletContext, reference);
    if (this.server != null && !this.handlers.containsKey(servletContext)) {
      try {
        registerServletContext(servletContext, reference);
      } catch (final Exception e) {
        LOGGER.error("Failed to register ServletContext", e);
      }
    }
  }

  /**
   * @param servletContext
   * @param reference
   */
  public synchronized void removeServletContext(final ServletContext servletContext,
      final ServiceReference<ServletContext> reference) {
    LOGGER.debug("Removing ServletContext: {}", servletContext);
    if (this.server != null && this.handlers.containsKey(servletContext)) {
      unregisterServletContext(servletContext);
    }
    this.servletContexts.remove(servletContext);
  }

  private void registerServletContext(final ServletContext servletContext,
      final ServiceReference<ServletContext> reference) throws Exception {
    final String contextId = reference.getProperty(ServletContext.CONTEXT_ID).toString();
    boolean deployed = false;
    if (this.handlerCollection.getHandlers() != null) {
      for (final Handler handler : this.handlerCollection.getHandlers()) {
        if (handler instanceof ServletContextHandler) {
          deployed |= ((ServletContextHandler) handler).getContextPath().equals(contextId);
        }
      }
    }
    if (!deployed) {
      final ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS
          | ServletContextHandler.SECURITY);
      servletContextHandler.setConnectorNames(servletContext.getConnectorNames());
      servletContextHandler.setContextPath(contextId);
      servletContextHandler.setVirtualHosts(servletContext.getVirtualHosts());
      for (final String key : reference.getPropertyKeys()) {
        if (key.startsWith("init.")) {
          servletContextHandler.setInitParameter(key.substring(5), reference.getProperty(key).toString());
        }
      }
      this.handlerCollection.addHandler(servletContextHandler);
      if (!servletContextHandler.isStarting() && !servletContextHandler.isStarted()) {
        servletContextHandler.start();
      }
      this.handlers.put(servletContext, servletContextHandler);
      this.registrator.addServletContext(servletContext, this.handlers.get(servletContext));
    }
  }

  private void unregisterServletContext(final ServletContext servletContext) {
    final ServletContextHandler handler = this.handlers.get(servletContext);
    if (handler != null) {
      try {
        this.registrator.removeServletContext(servletContext, handler);
        this.handlerCollection.removeHandler(handler);
        this.handlers.remove(servletContext);
      } catch (final Exception e) {
        LOGGER.error("Failed to unregister ServletContext", e);
      }
    }
  }

  Map<ServletContext, ServletContextHandler> getHandlers() {
    return this.handlers;
  }

  ServiceReference<ServletContext> getServletContextReference(final ServletContext servletContext) {
    return this.servletContexts.get(servletContext);
  }

}
