package org.centny.mgw.jgit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;

import org.centny.mgw.MgwLoader;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.IO;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.util.FS;

/**
 * security resolver serving from the local filesystem.
 * 
 * @author Centny.
 * 
 */
public class AuthorizationFileResolver implements
		RepositoryResolver<HttpServletRequest> {
	private volatile boolean exportAll;

	private final Map<String, Repository> exports;

	private final Collection<File> exportBase;

	/** Initialize an empty file based resolver. */
	public AuthorizationFileResolver() {
		exports = new ConcurrentHashMap<String, Repository>();
		exportBase = new CopyOnWriteArrayList<File>();
	}

	/**
	 * Create a new resolver for the given path.
	 * 
	 * @param basePath
	 *            the base path all repositories are rooted under.
	 * @param exportAll
	 *            if true, exports all repositories, ignoring the check for the
	 *            {@code git-daemon-export-ok} files.
	 */
	public AuthorizationFileResolver(final File basePath, final boolean exportAll) {
		this();
		exportDirectory(basePath);
		setExportAll(exportAll);
	}

	public Repository open(final HttpServletRequest req, final String name)
			throws RepositoryNotFoundException, ServiceNotEnabledException,
			ServiceNotAuthorizedException {
		if (isUnreasonableName(name))
			throw new RepositoryNotFoundException(name);

		Repository db = exports.get(nameWithDotGit(name));
		if (db != null) {
			db.incrementOpen();
			return db;
		}

		for (File base : exportBase) {
			File dir = FileKey.resolve(new File(base, name), FS.DETECTED);
			if (dir == null)
				continue;

			try {
				FileKey key = FileKey.exact(dir, FS.DETECTED);
				db = RepositoryCache.open(key, true);
			} catch (IOException e) {
				throw new RepositoryNotFoundException(name, e);
			}

			try {
				if (isExportOk(req, name, db)
						&& this.isAuthorizedOk(req, name, db)) {
					// We have to leak the open count to the caller, they
					// are responsible for closing the repository if we
					// complete successfully.
					return db;
				} else
					throw new ServiceNotEnabledException();

			} catch (RuntimeException e) {
				db.close();
				throw new RepositoryNotFoundException(name, e);

			} catch (IOException e) {
				db.close();
				throw new RepositoryNotFoundException(name, e);

			} catch (ServiceNotEnabledException e) {
				db.close();
				throw e;
			} catch (ServiceNotAuthorizedException e) {
				db.close();
				throw e;
			}
		}

		if (exportBase.size() == 1) {
			File dir = new File(exportBase.iterator().next(), name);
			throw new RepositoryNotFoundException(name,
					new RepositoryNotFoundException(dir));
		}

		throw new RepositoryNotFoundException(name);
	}

	/**
	 * @return false if <code>git-daemon-export-ok</code> is required to export
	 *         a repository; true if <code>git-daemon-export-ok</code> is
	 *         ignored.
	 * @see #setExportAll(boolean)
	 */
	public boolean isExportAll() {
		return exportAll;
	}

	/**
	 * Set whether or not to export all repositories.
	 * <p>
	 * If false (the default), repositories must have a
	 * <code>git-daemon-export-ok</code> file to be accessed through this
	 * daemon.
	 * <p>
	 * If true, all repositories are available through the daemon, whether or
	 * not <code>git-daemon-export-ok</code> exists.
	 * 
	 * @param export
	 */
	public void setExportAll(final boolean export) {
		exportAll = export;
	}

	/**
	 * Add a single repository to the set that is exported by this daemon.
	 * <p>
	 * The existence (or lack-thereof) of <code>git-daemon-export-ok</code> is
	 * ignored by this method. The repository is always published.
	 * 
	 * @param name
	 *            name the repository will be published under.
	 * @param db
	 *            the repository instance.
	 */
	public void exportRepository(String name, Repository db) {
		exports.put(nameWithDotGit(name), db);
	}

	/**
	 * Recursively export all Git repositories within a directory.
	 * 
	 * @param dir
	 *            the directory to export. This directory must not itself be a
	 *            git repository, but any directory below it which has a file
	 *            named <code>git-daemon-export-ok</code> will be published.
	 */
	public void exportDirectory(final File dir) {
		exportBase.add(dir);
	}

	/**
	 * Check if this repository can be served.
	 * <p>
	 * The default implementation of this method returns true only if either
	 * {@link #isExportAll()} is true, or the {@code git-daemon-export-ok} file
	 * is present in the repository's directory.
	 * 
	 * @param req
	 *            the current HTTP request.
	 * @param repositoryName
	 *            name of the repository, as present in the URL.
	 * @param db
	 *            the opened repository instance.
	 * @return true if the repository is accessible; false if not.
	 * @throws IOException
	 *             the repository could not be accessed, the caller will claim
	 *             the repository does not exist.
	 */
	protected boolean isExportOk(HttpServletRequest req, String repositoryName,
			Repository db) throws IOException {
		if (isExportAll())
			return true;
		else if (db.getDirectory() != null)
			return new File(db.getDirectory(), "git-daemon-export-ok").exists(); //$NON-NLS-1$
		else
			return false;
	}

	/**
	 * Check if have authorization for this repository.
	 * 
	 * @param req
	 *            the current HTTP request.
	 * @param repositoryName
	 *            name of the repository, as present in the URL.
	 * @param db
	 *            the opened repository instance.
	 * @return true if the repository is accessible; false if not.
	 * @throws IOException
	 *             the repository could not be accessed, the caller will claim
	 *             the repository does not exist.
	 */
	protected boolean isAuthorizedOk(HttpServletRequest req,
			String repositoryName, Repository db)
			throws ServiceNotAuthorizedException {
		HashLoginService hls = MgwLoader.sharedLoginService();
		if (hls == null) {
			throw new ServiceNotAuthorizedException();
		}
		File auth = new File(db.getDirectory(), ".authorized");
		if (!auth.exists()) {
			throw new ServiceNotAuthorizedException();
		}
		String uname = req.getRemoteUser();
		if (uname == null) {
			throw new ServiceNotAuthorizedException();
		}
		UserIdentity uity = hls.getUsers().get(uname);
		if (uity == null) {
			throw new ServiceNotAuthorizedException();
		}
		FileInputStream is = null;
		BufferedReader reader = null;
		try {
			is = new FileInputStream(auth);
			reader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			boolean isRole = false;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (line.equals("[role]")) {
					isRole = true;
					continue;
				} else if (line.equals("[user]")) {
					isRole = false;
					continue;
				}
				if (isRole) {
					if (uity.isUserInRole(line, null)) {
						return true;
					} else {
						continue;
					}
				} else {
					if (uname.equals(line)) {
						return true;
					} else {
						continue;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IO.close(is);
			IO.close(reader);
		}
		throw new ServiceNotAuthorizedException();
	}

	private static String nameWithDotGit(String name) {
		if (name.endsWith(Constants.DOT_GIT_EXT))
			return name;
		return name + Constants.DOT_GIT_EXT;
	}

	private static boolean isUnreasonableName(final String name) {
		if (name.length() == 0)
			return true; // no empty paths

		if (name.indexOf('\\') >= 0)
			return true; // no windows/dos style paths
		if (new File(name).isAbsolute())
			return true; // no absolute paths

		if (name.startsWith("../")) //$NON-NLS-1$
			return true; // no "l../etc/passwd"
		if (name.contains("/../")) //$NON-NLS-1$
			return true; // no "foo/../etc/passwd"
		if (name.contains("/./")) //$NON-NLS-1$
			return true; // "foo/./foo" is insane to ask
		if (name.contains("//")) //$NON-NLS-1$
			return true; // double slashes is sloppy, don't use it

		return false; // is a reasonable name
	}
}
