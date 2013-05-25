package org.centny.mgw;

import java.io.File;
import java.io.FileFilter;
import java.util.Properties;

import org.centny.jetty4a.server.api.EnvXml;

/**
 * external class for convert all xml file like *-generatable.xml by env.
 * 
 * @author Centny.
 * 
 */
public class XmlGenerator extends EnvXml {

	public XmlGenerator() {

	}

	public XmlGenerator(Properties ext) {
		super(ext);
	}

	/**
	 * convert all file in the folder.
	 * 
	 * @param dir
	 *            the target folder.
	 */
	public void convert(File dir) {
//		System.out.println(dir.getAbsolutePath());
		if (dir == null || !dir.exists()) {
			return;
		}
		File[] xmls = dir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				if (arg0.isDirectory()) {
					return false;
				}
				return arg0.getName().matches("^.*\\-generatable\\.xml$");
			}
		});
		if (xmls == null) {
			return;
		}
		for (File xml : xmls) {
			File dst = new File(dir, xml.getName().replace("-generatable", ""));
			this.convert(xml, dst);
		}
	}
}
