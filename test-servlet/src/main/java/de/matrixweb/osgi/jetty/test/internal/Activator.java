package de.matrixweb.osgi.jetty.test.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.matrixweb.osgi.jetty.context.DefaultServletContext;
import de.matrixweb.osgi.jetty.context.ServletContext;

public class Activator implements BundleActivator {

	private ServiceRegistration<ServletContext> reg;
	
	private ServiceRegistration<Servlet> reg2;

	private ServiceRegistration<Servlet> reg3;

	@Override
	public void start(BundleContext context) throws Exception {
		// ServletContext
		DefaultServletContext servletContext = new DefaultServletContext();
		servletContext.setContextPath("/");
		reg = context.registerService(ServletContext.class, servletContext, null);
		
		// Servlet
		Servlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
				PrintWriter writer = resp.getWriter();
				writer.write("Hello World!");
				writer.close();
			}
		};
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("alias", "/");
		props.put("contextId", "/");
		reg2 = context.registerService(Servlet.class, servlet, props);

		// Servlet
		servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
				PrintWriter writer = resp.getWriter();
				writer.write("Hello World " + getInitParameter("number") + "!");
				writer.close();
			}
		};
		props = new Hashtable<String, Object>();
		props.put("alias", "/msg");
		props.put("contextId", "/");
		props.put("init.number", 5);
		reg3 = context.registerService(Servlet.class, servlet, props);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (reg3 != null) {
			reg3.unregister();
			reg3 = null;
		}
		if (reg2 != null) {
			reg2.unregister();
			reg2 = null;
		}
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}

}
