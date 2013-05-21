package org.centny.mgw;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.centny.jge.JGitExt;
import org.centny.jge.amerge.AutoMerge;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

public class SyncMgr extends TimerTask {

	/**
	 * the single instance.
	 */
	private static SyncMgr smgr_;

	/**
	 * single instance method.
	 * 
	 * @return the instance.
	 */
	public static SyncMgr smgr() {
		if (smgr_ == null) {
			smgr_ = new SyncMgr();
		}
		return smgr_;
	}

	// the loop timer.
	private Timer timer;
	// the sync manager workspace directory.
	private File wsdir;
	// all AutoMerge instance.
	private Map<String, AutoMerge> amerges = new HashMap<String, AutoMerge>();
	// if sync to remote.
	private boolean sync2Remoete;

	/**
	 * private default constructor.
	 */
	private SyncMgr() {
		timer = new Timer();
	}

	/**
	 * start loop timer.
	 */
	public void startTimer() {
		this.timer.schedule(this, 0, 30000);
	}

	/**
	 * stop loop timer.
	 */
	public void stopTimer() {
		this.timer.cancel();
	}

	/**
	 * initial manager by workspace directory.
	 * 
	 * @param wsdir
	 *            the workspace directory.
	 */
	public void setWsDir(File wsdir) {
		this.wsdir = wsdir;
		if (!this.wsdir.exists()) {
			JGitExt.assertTrue(this.wsdir.mkdirs());
		}
		this.loadAMerges();
	}

	/**
	 * load all not loaded AutoMerge in the workspace.
	 */
	public synchronized void loadAMerges() {
		if (this.wsdir == null) {
			return;
		}
		File[] fs = this.wsdir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File dir) {
				return AutoMerge.isWsDir(dir);
			}
		});
		if (fs == null) {
			return;
		}
		for (File wsdir : fs) {
			if (this.amerges.containsKey(wsdir.getAbsolutePath())) {
				continue;
			}
			try {
				this.amerges.put(wsdir.getAbsolutePath(), new AutoMerge(wsdir));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * add an AutoMeerge to workspace.
	 * 
	 * @param name
	 *            AutoMerge name.
	 * @param local
	 *            the local repository URI.
	 * @param remote
	 *            the remote repository URI.
	 * @throws IOException
	 *             IO exception.
	 * @throws InvalidRemoteException
	 *             GIT exception.
	 * @throws TransportException
	 *             GIT exception.
	 * @throws GitAPIException
	 *             GIT exception.
	 */
	public synchronized void addAMerge(String name, String local, String remote)
			throws IOException, InvalidRemoteException, TransportException,
			GitAPIException {
		File wdir = new File(this.wsdir, name);
		AutoMerge am = new AutoMerge(wdir);
		am.cloneLocal(local, "master");
		am.cloneRemote(remote, "master");
		am.initAMerge();
		this.amerges.put(wdir.getAbsolutePath(), am);
	}

	/**
	 * get the AutoMerge by name.
	 * 
	 * @param name
	 *            the AutoMerge name.
	 * @return the AutoMerge instance.
	 */
	public synchronized AutoMerge amerge(String name) {
		File wdir = new File(this.wsdir, name);
		if (!wdir.exists()) {
			return null;
		}
		String wpath = wdir.getAbsolutePath();
		if (this.amerges.containsKey(wpath)) {
			return this.amerges.get(wpath);
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		for (String key : this.amerges.keySet()) {
			try {
				AutoMerge am = this.amerges.get(key);
				am.pullR2L();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * if sync local to remote.
	 * 
	 * @return true sync to remote,or not.
	 */
	public synchronized boolean isSync2Remoete() {
		return sync2Remoete;
	}

	/**
	 * set the sync to remote value.
	 * 
	 * @param sync2Remoete
	 *            true sync to remote,or not.
	 */
	public synchronized void setSync2Remoete(boolean sync2Remoete) {
		this.sync2Remoete = sync2Remoete;
	}
}
