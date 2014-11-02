package org.lazydoc.printer;

import org.apache.commons.io.FileUtils;
import org.lazydoc.config.PrinterConfig;
import org.lazydoc.model.DocDataType;
import org.lazydoc.model.DocDomain;
import org.lazydoc.model.DocError;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public abstract class DocumentationPrinter {

	protected Map<String, String> files = new TreeMap<String, String>();
    protected PrinterConfig printerConfig;

	public abstract void print(PrinterConfig printerConfig) throws Exception;

	protected void writeFiles(String target) throws IOException {
		for (String filename : files.keySet()) {
			File file = new File(target + filename);
			file.getParentFile().mkdirs();
			System.out.println("Writing file " + file.getAbsolutePath());
			FileUtils.write(file, files.get(filename), Charset.forName("UTF-8"));
		}
	}

}