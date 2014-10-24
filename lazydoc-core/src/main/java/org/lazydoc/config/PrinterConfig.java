package org.lazydoc.config;

public class PrinterConfig {
	
	private String className;
	private String outputPath;
	
	public PrinterConfig(String className, String outputPath) {
		super();
		this.className = className;
		this.outputPath = outputPath;
	}
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getOutputPath() {
		return outputPath;
	}
	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	
	

}
