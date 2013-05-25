package org.centny.mgw.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jgit.internal.storage.file.FileRepository;

public class RepoMgrServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3600259257847149848L;

	/**
	 * the repository root folder.
	 */
	private File repoDir;

	/**
	 * the logger.
	 */
	private Logger log = Log.getLogger(RepoMgrServlet.class);

	/**
	 * @return the repoDir
	 */
	public File getRepoDir() {
		return repoDir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		String rdpath = config.getInitParameter("base-path");
		if (rdpath == null || rdpath.trim().isEmpty()) {
			throw new ServletException("repository base path is not setted.");
		}
		this.repoDir = new File(rdpath);
		if (!this.repoDir.exists()) {
			throw new ServletException("repository base path is not found.");
		}
		this.log.debug("initial repository manager by base path:"
				+ this.repoDir.getAbsolutePath());
	}

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
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "empty action");
			return;
		}
		String[] pathes = pinfo.split("\\/");
		if (pathes == null || pathes.length < 1) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "empty action");
			return;
		}
		List<String> cmds = new ArrayList<String>();
		for (String cmd : pathes) {
			if (cmd.isEmpty()) {
				continue;
			}
			cmds.add(cmd);
		}
		this.doCmds(req, resp, cmds);
	}

	protected void doCmds(HttpServletRequest req, HttpServletResponse resp,
			List<String> cmds) throws IOException {
		if (cmds.isEmpty()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "empty action");
			return;
		}
		String cmd = cmds.get(0);
		if (cmd.equals("add")) {
			this.addRepositroy(req, resp, cmds);
		}
	}

	private void addRepositroy(HttpServletRequest req,
			HttpServletResponse resp, List<String> cmds) throws IOException {
		if (cmds.size() < 2) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"repository name must be setted.");
			return;
		}
		String rname = cmds.get(1);
		if (!rname.matches("^.*\\.git$")) {
			rname += ".git";
		}
		File repo = new File(this.repoDir, rname);
		if (repo.exists()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"repository aleady exist.");
			return;
		}
		try {
			FileRepository nrepo = new FileRepository(repo);
			nrepo.create();
			resp.getOutputStream().write("OK".getBytes());
		} catch (Exception e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"create repository error:" + e.toString());
		}
	}
}
