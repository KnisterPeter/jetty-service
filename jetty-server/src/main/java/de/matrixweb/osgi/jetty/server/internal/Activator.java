package de.matrixweb.osgi.jetty.server.internal;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.matrixweb.osgi.jetty.api.ServletContext;
import de.matrixweb.osgi.jetty.server.internal.RegisteringServiceTracker.RegistrationCaller;

/**
 * @author markusw
 */
public class Activator implements BundleActivator {

	private Jetty jetty;

	private Registrator registrator;

	private List<RegisteringServiceTracker<?>> trackers = new ArrayList<RegisteringServiceTracker<?>>();

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		registrator = new Registrator();
		jetty = new Jetty(context, registrator);

		createServletContextTracker(context);
		createFilterTracker(context);
		createServletTracker(context);

		for (RegisteringServiceTracker<?> tracker : trackers) {
			tracker.open();
		}
	}

	private void createServletContextTracker(BundleContext context) {
		trackers.add(new RegisteringServiceTracker<ServletContext>(context,
				ServletContext.class, new RegistrationCaller<ServletContext>() {
					@Override
					public void add(ServiceReference<ServletContext> reference,
							ServletContext service) {
						jetty.addServletContext(service, reference);
					}

					@Override
					public void remove(
							ServiceReference<ServletContext> reference,
							ServletContext service) {
						jetty.removeServletContext(service, reference);
					}
				}));
	}

	private void createFilterTracker(BundleContext context) {
		trackers.add(new RegisteringServiceTracker<Filter>(context,
				Filter.class, new RegistrationCaller<Filter>() {
					@Override
					public void add(ServiceReference<Filter> reference,
							Filter service) {
						registrator.addFilter(reference, service);
					}

					@Override
					public void remove(ServiceReference<Filter> reference,
							Filter service) {
						registrator.removeFilter(reference, service);
					}
				}));
	}

	private void createServletTracker(BundleContext context) {
		trackers.add(new RegisteringServiceTracker<Servlet>(context,
				Servlet.class, new RegistrationCaller<Servlet>() {
					@Override
					public void add(ServiceReference<Servlet> reference,
							Servlet service) {
						registrator.addServlet(reference, service);
					}

					@Override
					public void remove(ServiceReference<Servlet> reference,
							Servlet service) {
						registrator.removeServlet(reference, service);
					}
				}));
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		for (RegisteringServiceTracker<?> tracker : trackers) {
			tracker.close();
		}
		trackers.clear();
		if (this.jetty != null) {
			this.jetty.dispose();
			jetty = null;
		}
	}

}
