package de.matrixweb.osgi.jetty.server.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matrixweb.osgi.jetty.context.ServletContext;

/**
 * @author marwol
 */
public class Jetty implements ManagedService {

	private static final Logger LOGGER = LoggerFactory.getLogger(Jetty.class);

	private ServiceRegistration<ManagedService> registration;

	private Server server;

	private ServletRegistrator servletRegistrator;

	private Map<ServletContext, ServletContextHandler> handlers = new HashMap<ServletContext, ServletContextHandler>();

	public Jetty(BundleContext context,
			ServletRegistrator servletRegistrator) {
		this.servletRegistrator = servletRegistrator;

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
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		try {
			if (server != null) {
				server.stop();
				server = null;
			}
			if (properties != null) {
				server = new Server();

				final SelectChannelConnector connector = new SelectChannelConnector();
				connector.setName(getString(properties, "connector", null));
				connector.setHost(getString(properties, "host", null));
				connector.setPort(getInteger(properties, "port", 8181));
				server.addConnector(connector);
				server.setHandler(new ContextHandlerCollection());
				server.start();
				LOGGER.info("Started jetty server");

				// TODO: Refresh servletregistrator
			} else {
				LOGGER.info("Stopped jetty server");
			}
		} catch (Exception e) {
			throw new ConfigurationException("unknown", "unknown", e);
		}
		registration.setProperties(properties);
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

	public void addServletContext(ServletContext servletContext) {
		LOGGER.info("Adding ServletContext: {}", servletContext);
		try {
			servletRegistrator.addServletContext(servletContext,
					registerServletContext(servletContext));
		} catch (Exception e) {
			LOGGER.error("Failed to register ServletContext", e);
		}
	}

	private ServletContextHandler registerServletContext(
			ServletContext servletContext) throws Exception {
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.setConnectorNames(servletContext
				.getConnectorNames());
		servletContextHandler.setContextPath(servletContext.getContextPath());
		servletContextHandler.setVirtualHosts(servletContext.getVirtualHosts());
		for (Entry<String, String> entry : servletContext.getInitParameters()
				.entrySet()) {
			servletContextHandler.setInitParameter(entry.getKey(),
					entry.getValue());
		}

		((ContextHandlerCollection) server.getHandler())
				.addHandler(servletContextHandler);
		servletContextHandler.start();
		servletRegistrator.addServletContext(servletContext,
				servletContextHandler);

		handlers.put(servletContext, servletContextHandler);
		return servletContextHandler;
	}

	public void removeServletContext(ServletContext servletContext) {
		LOGGER.info("Removing ServletContext: {}", servletContext);
		try {
			ServletContextHandler handler = handlers.remove(servletContext);
			servletRegistrator.removeServletContext(servletContext, handler);
			((ContextHandlerCollection) server.getHandler())
					.removeHandler(handler);
		} catch (Exception e) {
			LOGGER.error("Failed to unregister ServletContext", e);
		}
	}

}
