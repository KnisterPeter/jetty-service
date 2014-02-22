package de.matrixweb.osgi.jetty.server.internal;

import javax.servlet.Servlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ServletServiceTracker extends ServiceTracker<Servlet, Servlet> {

	private ServletRegistrator servletRegistrator;

	public ServletServiceTracker(BundleContext context, ServletRegistrator servletRegistrator) {
		super(context, Servlet.class, null);
		this.servletRegistrator = servletRegistrator;
	}

	@Override
	public Servlet addingService(ServiceReference<Servlet> reference) {
		Servlet servlet = super.addingService(reference);
		servletRegistrator.addServlet(reference, servlet);
		return servlet;
	}
	
	@Override
	public void removedService(ServiceReference<Servlet> reference,
			Servlet service) {
		servletRegistrator.removeServlet(reference, service);
		super.removedService(reference, service);
	}
	
}
