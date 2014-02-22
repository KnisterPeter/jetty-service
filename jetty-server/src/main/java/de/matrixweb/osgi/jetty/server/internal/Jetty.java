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

	private Registrator registrator;

	private Map<ServletContext, ServiceReference<ServletContext>> servletContexts = Collections
			.synchronizedMap(new HashMap<ServletContext, ServiceReference<ServletContext>>());

	private Map<ServletContext, ServletContextHandler> handlers = new HashMap<ServletContext, ServletContextHandler>();

	public Jetty(BundleContext context, Registrator registrator) {
		this.registrator = registrator;
		registrator.setJetty(this);

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(Constants.SERVICE_PID, "jettyservice");
		registration = context.registerService(ManagedService.class, this,
				props);
	}

	public void dispose() {
		if (registration != null) {
			try {
				updated(null);
			} catch (ConfigurationException e) {
				LOGGER.error("Failed to stop jetty server", e);
			}
			registration.unregister();
			registration = null;
		}
	}

	@Override
	public synchronized void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		try {
			if (server != null) {
				server.stop();
				server = null;
				handlerCollection = null;
				handlers.clear();
			}
			if (properties != null) {
				server = new Server();

				final SelectChannelConnector connector = new SelectChannelConnector();
				connector.setName(getString(properties, "connector", null));
				connector.setHost(getString(properties, "host", null));
				connector.setPort(getInteger(properties, "port", 8181));
				server.addConnector(connector);
				handlerCollection = new ContextHandlerCollection();
				server.setHandler(handlerCollection);
				server.start();
				LOGGER.info("Started jetty server");

				for (Entry<ServletContext, ServiceReference<ServletContext>> entry : servletContexts
						.entrySet()) {
					registerServletContext(entry.getKey(), entry.getValue());
				}
				registrator.refresh();
			} else {
				LOGGER.info("Stopped jetty server");
			}
		} catch (Exception e) {
			throw new ConfigurationException("unknown", "unknown", e);
		}
		if (registration != null) {
			registration.setProperties(properties);
		}
	}

	private String getString(Dictionary<String, ?> properties, String key,
			String ifNull) {
		Object value = properties.get(key);
		return value != null ? value.toString() : ifNull;
	}

	private Integer getInteger(Dictionary<String, ?> properties, String key,
			Integer ifNull) {
		Object value = properties.get(key);
		return value != null ? Integer.valueOf(value.toString()) : ifNull;
	}

	public synchronized void addServletContext(ServletContext servletContext,
			ServiceReference<ServletContext> reference) {
		LOGGER.info("Adding ServletContext: {}", servletContext);
		servletContexts.put(servletContext, reference);
		if (server != null && !handlers.containsKey(servletContext)) {
			try {
				registerServletContext(servletContext, reference);
				registrator.addServletContext(servletContext,
						handlers.get(servletContext));
			} catch (Exception e) {
				LOGGER.error("Failed to register ServletContext", e);
			}
		}
	}

	public synchronized void removeServletContext(
			ServletContext servletContext,
			ServiceReference<ServletContext> reference) {
		LOGGER.info("Removing ServletContext: {}", servletContext);
		if (server != null && handlers.containsKey(servletContext)) {
			unregisterServletContext(servletContext);
		}
		servletContexts.remove(servletContext);
	}

	private void registerServletContext(ServletContext servletContext,
			ServiceReference<ServletContext> reference) throws Exception {
		String contextId = reference.getProperty(ServletContext.CONTEXT_ID)
				.toString();
		boolean deployed = false;
		if (handlerCollection.getHandlers() != null) {
			for (Handler handler : handlerCollection.getHandlers()) {
				if (handler instanceof ServletContextHandler) {
					deployed |= ((ServletContextHandler) handler)
							.getContextPath().equals(contextId);
				}
			}
		}
		if (!deployed) {
			ServletContextHandler servletContextHandler = new ServletContextHandler(
					ServletContextHandler.SESSIONS
							| ServletContextHandler.SECURITY);
			servletContextHandler.setConnectorNames(servletContext
					.getConnectorNames());
			servletContextHandler.setContextPath(contextId);
			servletContextHandler.setVirtualHosts(servletContext
					.getVirtualHosts());
			for (String key : reference.getPropertyKeys()) {
				if (key.startsWith("init.")) {
					servletContextHandler.setInitParameter(key.substring(5),
							reference.getProperty(key).toString());
				}
			}
			handlerCollection.addHandler(servletContextHandler);
			if (!servletContextHandler.isStarting()
					&& !servletContextHandler.isStarted()) {
				servletContextHandler.start();
			}
			handlers.put(servletContext, servletContextHandler);
		} else {
			LOGGER.warn("Already registered servlet-context: FIX DOUBLE REG");
		}
	}

	private void unregisterServletContext(ServletContext servletContext) {
		ServletContextHandler handler = handlers.get(servletContext);
		if (handler != null) {
			try {
				registrator.removeServletContext(servletContext, handler);
				handlerCollection.removeHandler(handler);
				handlers.remove(servletContext);
			} catch (Exception e) {
				LOGGER.error("Failed to unregister ServletContext", e);
			}
		}
	}

	public Map<ServletContext, ServletContextHandler> getHandlers() {
		return handlers;
	}

	public ServiceReference<ServletContext> getServletContextReference(
			ServletContext servletContext) {
		return servletContexts.get(servletContext);
	}

}
