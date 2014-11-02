package org.lazydoc.example.swagger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "org.lazydoc.example.swagger")
@Import(SwaggerMvcConfig.class)
public class SwaggerExampleApplication extends SpringBootServletInitializer {

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.setPort(9990);
        factory.setContextPath("/lazydoc-swaggerexample");
        return factory;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SwaggerExampleApplication.class);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SwaggerExampleApplication.class, args);
    }
}
