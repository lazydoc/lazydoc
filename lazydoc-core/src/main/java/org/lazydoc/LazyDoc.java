package org.lazydoc;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.lazydoc.config.Config;
import org.lazydoc.config.PrinterConfig;
import org.lazydoc.parser.DataTypeParser;
import org.lazydoc.parser.spring.SpringParser;
import org.lazydoc.printer.DocumentationPrinter;
import org.lazydoc.reporter.DocumentationReporter;

import java.util.List;

public class LazyDoc {

    private DocumentationReporter reporter;
	private DataTypeParser dataTypeParser;
	private SpringParser springParser;
	private Config config;
	

	public void document(Config config, List printerConfigs, String logLevel) throws Exception {
		this.config = config;
		this.reporter = new DocumentationReporter();
		this.dataTypeParser = new DataTypeParser(reporter, config);
		this.springParser = new SpringParser(config, reporter, dataTypeParser);

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(Level.getLevel(logLevel));
		ctx.updateLoggers();

		springParser.parseSpringControllers();
		if (printerConfigs != null) {
			for(PrinterConfig printerConfig : (List<PrinterConfig>)printerConfigs) {
				printerConfig.setDomains(springParser.getDomains());
				printerConfig.setDataTypes(dataTypeParser.getDataTypes());
				printerConfig.setListOfCommonErrors(springParser.getListOfCommonErrors());
				printerConfig.setOutputPath(printerConfig.getOutputPath());
				DocumentationPrinter printer = (DocumentationPrinter)Class.forName(printerConfig.getClassName()).newInstance();
				printer.print(printerConfig);
			}
		}
		reporter.printOverallProgressReport();
        if(config.isBreakOnUndocumented() && reporter.getUndocumentedCount() > 0) {
            throw new RuntimeException("There are undocumented methods, errorhandlers or fields. Please see report.");
        }
	}

}
