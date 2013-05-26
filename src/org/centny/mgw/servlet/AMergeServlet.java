package org.centny.mgw.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.centny.mgw.SyncMgr;

public class AMergeServlet extends CmdServlet {

	/**
	 * serial version id.
	 */
	private static final long serialVersionUID = -5001924646257295953L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.centny.mgw.servlet.CmdServlet#doCmds(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse,
	 * java.util.List)
	 */
	@Override
	protected void doCmds(HttpServletRequest req, HttpServletResponse resp,
			List<String> cmds) throws ServletException, IOException {
		if (cmds.get(0).equals("add")) {
			this.addAMerge(req, resp, cmds);
		}
	}

	/**
	 * @param req
	 *            http request.
	 * @param resp
	 *            http response.
	 * @param cmds
	 *            the execute command and parameter.
	 * @throws ServletException
	 *             error exception.
	 * @throws IOException
	 *             error exception.
	 * 
	 */
	private void addAMerge(HttpServletRequest req, HttpServletResponse resp,
			List<String> cmds) throws IOException, ServletException {
		if (cmds.size() < 2) {
			this.sendBadRequest(resp,
					"Usage:amerge/add/<name>?local=<local addres>&remote=<remote address>");
			return;
		}
		String name = cmds.get(1);
		String local = req.getParameter("local");
		String remote = req.getParameter("remote");
		if (local == null || remote == null) {
			this.sendBadRequest(resp, "local and remote address is not setted.");
			return;
		}
		try {
			// File tt = new File("/tmp/tt");
			// if (tt.exists()) {
			// FileUtils.deleteDirectory(tt);
			// }
			// JGitExt.clone(tt, local);
			SyncMgr.smgr().addAMerge(name, local, remote);
			resp.getOutputStream().write("OK".getBytes());
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}
