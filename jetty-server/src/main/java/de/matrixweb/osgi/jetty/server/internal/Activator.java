package de.matrixweb.osgi.jetty.server.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author markusw
 */
public class Activator implements BundleActivator {

	private Jetty jetty;

	private ServletRegistrator servletRegistrator;
	
	private ServletContextHandlerServiceTracker servletContextHandlerServiceTracker;

	private ServletServiceTracker servletServiceTracker;

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		servletRegistrator = new ServletRegistrator();
		
		jetty = new Jetty(context, servletRegistrator);

		servletContextHandlerServiceTracker = new ServletContextHandlerServiceTracker(context, jetty);
		servletContextHandlerServiceTracker.open();
		
		servletServiceTracker = new ServletServiceTracker(context, servletRegistrator);
		servletServiceTracker.open();
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		if (servletServiceTracker != null) {
			servletServiceTracker.close();
			servletServiceTracker = null;
		}
		if (servletContextHandlerServiceTracker != null) {
			servletContextHandlerServiceTracker.close();
			servletContextHandlerServiceTracker = null;
		}
		if (this.jetty != null) {
			this.jetty.dispose();
			jetty = null;
		}
	}

}
