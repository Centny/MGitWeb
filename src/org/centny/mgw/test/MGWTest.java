package org.centny.mgw.test;

import java.io.File;
import java.util.Properties;

import org.centny.jetty4a.server.api.JettyServer;
import org.centny.jetty4a.server.dev.JettyDevServer;
import org.centny.mgw.XmlGenerator;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Test;

public class MGWTest {
	// static {
	// URL url = MGWTest.class.getClassLoader().getResource(
	// "log4jetty.properties");
	// if (url != null) {
	// InputStream is = null;
	// try {
	// is = url.openStream();
	// System.getProperties().load(is);
	// } catch (IOException e) {
	// }
	// IO.close(is);
	// }
	// }

	// @Test
	public void testXmlGenerator() {
		Logger log = Log.getLogger(this.getClass());
		log.debug("ssssss");
		JettyDevServer.initWebDev();
		Properties ext = new Properties();
		ext.setProperty("REPO_DIR", "/ssss");
		new XmlGenerator(ext).convert(new File("WebContent/WEB-INF"));
	}

	@Test
	public void testMgw() throws Exception {
		JettyDevServer.initWebDev();
		System.getProperties().setProperty("MGIT_DIR", "Jetty4ADev");
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(10);
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setThreadPool(threadPool);
		connector.setPort(8080);
		JettyServer jds = JettyServer.createServer(JettyDevServer.class);
		jds.addConnector(connector);
//		jds.setThreadPool(threadPool);
		jds.start();
		jds.join();
	}
}
