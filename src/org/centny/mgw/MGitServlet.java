package org.centny.mgw;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.eclipse.jgit.http.server.GitSmartHttpTools.sendError;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.centny.jge.amerge.AutoMerge;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.http.server.GitSmartHttpTools;

public class MGitServlet extends GitServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5157514874051960832L;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		SyncMgr.smgr().setWsDir(new File("/tmp/mgit"));
		SyncMgr.smgr().setSync2Remoete(true);
		AutoMerge am = SyncMgr.smgr().amerge("jgd");
		if (am == null) {
			try {
				SyncMgr.smgr().addAMerge("jgd",
						"file:///Users/Scorpion/Temp/jgd.git",
						"file:///Users/Scorpion/Temp/jgdt.git");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		super.init(config);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {
		String name = req.getPathInfo();
		while (name != null && 0 < name.length() && name.charAt(0) == '/')
			name = name.substring(1);
		if (name == null || name.length() == 0) {
			sendError(req, res, SC_NOT_FOUND);
			return;
		}
		name = name.replaceAll("\\.git.*$", "");
		AutoMerge am = SyncMgr.smgr().amerge(name);
		if (am != null) {
			if (SyncMgr.smgr().isSync2Remoete()) {
				try {
					am.pullR2L();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (am.isConflict()) {
				sendError(req, res, SC_INTERNAL_SERVER_ERROR,
						"repository conflicting...");
				return;
			}
		}
//		System.out.println(req.getPathInfo());
		super.service(req, res);
		if (GitSmartHttpTools.isReceivePack(req) && am != null
				&& SyncMgr.smgr().isSync2Remoete()) {
			try {
				am.checkLogAndL2R();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
