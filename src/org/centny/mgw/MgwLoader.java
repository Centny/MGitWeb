package org.centny.mgw;

import java.io.File;
import java.util.Properties;

import org.centny.jetty4a.server.api.ServerListener;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * the MGit loader.
 * 
 * @author Centny.
 * 
 */
public class MgwLoader extends ServerListener {

	/**
	 * the shared login service.
	 */
	private static HashLoginService sharedLoginService_ = null;

	/**
	 * get the shared login service instance.
	 * 
	 * @return the login service.
	 */
	public static HashLoginService sharedLoginService() {
		return sharedLoginService_;
	}

	/**
	 * serial version id.
	 */
	private static final long serialVersionUID = 8118005376469706978L;
	/**
	 * the hash login service.
	 */
	private HashLoginService hls;

	/**
	 * the logger.
	 */
	private Logger log = Log.getLogger(MgwLoader.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.centny.jetty4a.server.api.ServerListener#init(java.io.File,
	 * java.io.File, java.io.File)
	 */
	@Override
	public void init(File root, File croot, File droot, Properties webp) {
		//
		new XmlGenerator(webp).convert(croot);
		//
		File realm = null;
		if (webp.containsKey("REALM")) {
			realm = new File(webp.getProperty("REALM"));
		} else {
			realm = new File(croot, "Realm.properties");
		}
		if (!realm.exists()) {
			this.log.warn("the realm configure file " + realm.getAbsolutePath()
					+ " not exist");
			return;
		}
		try {
			this.hls = new HashLoginService("MGit Login");
			this.hls.setConfig(realm.getAbsolutePath());
			this.hls.start();
			this.log.debug("using realm configure file:"
					+ realm.getAbsolutePath());
			sharedLoginService_ = this.hls;
		} catch (Exception e) {
			this.log.warn(e);
		}
		String sws = webp.getProperty("SYNC_WS");
		if (sws != null && sws.trim().length() > 0) {
			SyncMgr.smgr().setWsDir(new File(sws));
			SyncMgr.smgr().startTimer();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.centny.jetty4a.server.api.ServerListener#create(java.lang.ClassLoader
	 * , java.util.Properties)
	 */
	@Override
	public Handler create(ClassLoader loader, Properties webp) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.centny.jetty4a.server.api.ServerListener#initWebApp(org.eclipse.jetty
	 * .webapp.WebAppContext, java.util.Properties)
	 */
	@Override
	public void initWebApp(WebAppContext wapp, Properties webp) {
		if (this.hls != null) {
			wapp.getSecurityHandler().setLoginService(this.hls);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.centny.jetty4a.server.api.ServerListener#destroy()
	 */
	@Override
	public void destroy() {
		if (this.hls != null) {
			try {
				this.hls.stop();
			} catch (Exception e) {
				this.log.warn(e);
			}
		}
		if (SyncMgr.smgr().isTimerStarted()) {
			SyncMgr.smgr().stopTimer();
		}
	}

	/**
	 * @return the hls
	 */
	public HashLoginService getHls() {
		return hls;
	}
}
