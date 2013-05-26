package org.centny.mgw.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class CmdServlet extends HttpServlet {

	/**
	 * serial version id.
	 */
	private static final long serialVersionUID = -2225503332314629778L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		this.doPost(req, resp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String pinfo = req.getPathInfo();
		if (pinfo == null || pinfo.trim().isEmpty()) {
			this.sendBadRequest(resp, "empty action");
			return;
		}
		String[] pathes = pinfo.split("\\/");
		if (pathes == null || pathes.length < 1) {
			this.sendBadRequest(resp, "empty action");
			return;
		}
		List<String> cmds = new ArrayList<String>();
		for (String cmd : pathes) {
			if (cmd.isEmpty()) {
				continue;
			}
			cmds.add(cmd);
		}
		if (cmds.isEmpty()) {
			this.sendBadRequest(resp, "empty action");
			return;
		}
		this.doCmds(req, resp, cmds);
	}

	/**
	 * @param resp
	 *            http response.
	 * @param msg
	 *            the message.
	 * @throws IOException
	 *             the error exception.
	 */
	public void sendBadRequest(HttpServletResponse resp, String msg)
			throws IOException {
		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
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
	protected abstract void doCmds(HttpServletRequest req,
			HttpServletResponse resp, List<String> cmds)
			throws ServletException, IOException;

}
