package de.matrixweb.osgi.jetty.server.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import de.matrixweb.osgi.jetty.api.ServletContext;

public class ServletContextHandlerServiceTracker extends
		ServiceTracker<ServletContext, ServletContext> {

	private Jetty jetty;

	public ServletContextHandlerServiceTracker(BundleContext context,
			Jetty jetty) {
		super(context, ServletContext.class, null);
		this.jetty = jetty;
	}

	@Override
	public ServletContext addingService(
			ServiceReference<ServletContext> reference) {
		ServletContext servletContext = super.addingService(reference);
		jetty.addServletContext(servletContext, reference);
		return servletContext;
	}

	@Override
	public void removedService(ServiceReference<ServletContext> reference,
			ServletContext service) {
		jetty.removeServletContext(service, reference);
		super.removedService(reference, service);
	}

}
