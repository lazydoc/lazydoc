package org.lazydoc.example.spring.doc;

import org.lazydoc.config.Config;
import org.lazydoc.config.PrinterConfig;
import org.lazydoc.LazyDoc;

import java.util.HashMap;

public class ExampleDocParser {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.setBreakOnUndocumented(true);
        config.setPackageToSearchForControllers("org.lazydoc.example");
        config.setDocumentationSuffix("Documentation");
        config.getPrinterConfigs().add(new PrinterConfig("org.lazydoc.printer.DocBookDocumentationPrinter", args[0] + "/src/main/resources/docbook/"));
        String swaggerExampleBasePath = args[0] + "/../lazydoc-swaggersample/src/main/";
        HashMap<String, String> swaggerParams = new HashMap<String, String>();
        swaggerParams.put("swagger.basepath", "http://localhost:9999/lazydoc-spring");
        swaggerParams.put("swagger.views.properties.filename", swaggerExampleBasePath + "resources/views.properties");
        swaggerParams.put("swagger.directory.name", "api");

        config.getPrinterConfigs().add(new PrinterConfig("org.lazydoc.printer.SwaggerDocumentationPrinter", swaggerExampleBasePath+"webapp/WEB-INF/jsp/swagger/", swaggerParams));
        new LazyDoc(config).document();
    }
}
