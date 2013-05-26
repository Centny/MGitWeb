package org.centny.mgw.servlet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jgit.internal.storage.file.FileRepository;

/**
 * the repository manager.
 * 
 * @author Centny.
 * 
 */
public class RepoMgrServlet extends CmdServlet {

	/**
	 * serial version id.
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
	 * @see org.centny.mgw.servlet.CmdServlet#doCmds(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse,
	 * java.util.List)
	 */
	@Override
	protected void doCmds(HttpServletRequest req, HttpServletResponse resp,
			List<String> cmds) throws IOException {
		String cmd = cmds.get(0);
		if (cmd.equals("add")) {
			this.addRepositroy(req, resp, cmds);
		}
	}

	/**
	 * add a repository.
	 * 
	 * @param req
	 *            http request.
	 * @param resp
	 *            http response.
	 * @param cmds
	 *            the add command and parameter.
	 * @throws IOException
	 *             error exception.
	 */
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
