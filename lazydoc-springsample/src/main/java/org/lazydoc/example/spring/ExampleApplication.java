package org.lazydoc.example.spring;

import org.lazydoc.example.spring.config.DispatcherConfig;
import org.lazydoc.example.spring.config.PersistenceConfig;
import org.lazydoc.example.spring.config.WebInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "org.lazydoc.example.spring")
@Import({ WebInitializer.class, DispatcherConfig.class, PersistenceConfig.class })
public class ExampleApplication {

	@Autowired
	private Environment env;

    @Bean
	public EmbeddedServletContainerFactory servletContainer() {
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		factory.setPort(9999);
		factory.setContextPath("/lazydoc-spring");
		return factory;
	}

	public static void main(String[] args) {
		SpringApplication.run(ExampleApplication.class, args);
	}
}
