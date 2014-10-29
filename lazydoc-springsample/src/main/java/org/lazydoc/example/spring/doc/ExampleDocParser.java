package org.lazydoc.example.spring.doc;

import org.lazydoc.config.Config;
import org.lazydoc.parser.DocumentationParser;

import java.net.URL;
import java.net.URLClassLoader;

public class ExampleDocParser {

    public static void main(String[] args) throws Exception {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();


        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
            System.out.println(url.getFile());
        }

        Class.forName("org.lazydoc.example.spring.controller.RestfulCustomerController");
        Config config = new Config();
        config.setPackageToSearchForControllers("org.lazydoc.example.spring");
        new DocumentationParser(config).parseDocumentation();
    }

}
