package de.matrixweb.osgi.jetty.internal;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author markusw
 */
public class Activator implements BundleActivator {

  private Server jetty;

  /**
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(final BundleContext context) throws Exception {
    System.out.println("Starting jetty");
    this.jetty = new Server();

    final SelectChannelConnector connector = new SelectChannelConnector();
    connector.setHost("localhost");
    connector.setPort(8080);
    this.jetty.addConnector(connector);

    this.jetty.start();
    System.out.println("Started jetty");
  }

  /**
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(final BundleContext context) throws Exception {
    if (this.jetty != null) {
      System.out.println("Stopping jetty");
      this.jetty.stop();
      this.jetty = null;
      System.out.println("Stopped jetty");
    }
  }

}
