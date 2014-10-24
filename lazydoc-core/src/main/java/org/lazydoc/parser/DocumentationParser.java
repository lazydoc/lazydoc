package org.lazydoc.parser;

import org.lazydoc.config.Config;
import org.lazydoc.config.PrinterConfig;
import org.lazydoc.parser.spring.SpringParser;
import org.lazydoc.printer.DocumentationPrinter;
import org.lazydoc.reporter.DocumentationReporter;

public class DocumentationParser {

    private DocumentationReporter reporter;
	private DataTypeParser dataTypeParser;
	private SpringParser springParser;
	private Config config;
	

	public DocumentationParser(Config config) {
		this.config = config;
		this.reporter = new DocumentationReporter();
		this.dataTypeParser = new DataTypeParser(reporter, config.getBaseDTOClassname());
		this.springParser = new SpringParser(config, reporter, dataTypeParser);
	}

	
	public void parseDocumentation() throws Exception {
		springParser.parseSpringControllers();
		for(PrinterConfig printerConfig : config.getPrinterConfigs()) {
			DocumentationPrinter printer = (DocumentationPrinter)Class.forName(printerConfig.getClassName()).newInstance();
			printer.print(springParser.getDomains(), dataTypeParser.getDataTypes(), printerConfig.getOutputPath(), springParser.getListOfCommonErrors());;
		}
		reporter.printOverallProgressReport();
	}

}
