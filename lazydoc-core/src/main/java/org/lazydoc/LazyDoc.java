package org.lazydoc;

import org.lazydoc.config.Config;
import org.lazydoc.config.PrinterConfig;
import org.lazydoc.parser.DataTypeParser;
import org.lazydoc.parser.spring.SpringParser;
import org.lazydoc.printer.DocumentationPrinter;
import org.lazydoc.reporter.DocumentationReporter;

public class LazyDoc {

    private DocumentationReporter reporter;
	private DataTypeParser dataTypeParser;
	private SpringParser springParser;
	private Config config;
	

	public LazyDoc(Config config) {
		this.config = config;
		this.reporter = new DocumentationReporter();
		this.dataTypeParser = new DataTypeParser(reporter, config.getBaseDTOClassname());
		this.springParser = new SpringParser(config, reporter, dataTypeParser);
	}

	
	public void document() throws Exception {
		springParser.parseSpringControllers();
		for(PrinterConfig printerConfig : config.getPrinterConfigs()) {
            printerConfig.setDomains(springParser.getDomains());
            printerConfig.setDataTypes(dataTypeParser.getDataTypes());
            printerConfig.setListOfCommonErrors(springParser.getListOfCommonErrors());
            printerConfig.setOutputPath(printerConfig.getOutputPath());
			DocumentationPrinter printer = (DocumentationPrinter)Class.forName(printerConfig.getClassName()).newInstance();
			printer.print(printerConfig);
		}
		reporter.printOverallProgressReport();
        if(config.isBreakOnUndocumented() && reporter.getUndocumentedCount() > 0) {
            throw new RuntimeException("There are undocumented methods, errorhandlers or fields. Please see report.");
        }
	}

}
