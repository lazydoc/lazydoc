package org.lazydoc.example.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.io.IOException;
import java.util.Properties;

@Configuration
@EnableWebMvc
public class SwaggerMvcConfig extends WebMvcConfigurerAdapter {

    @Bean
    public InternalResourceViewResolver getInternalResourceViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        try {
            registry.addViewController("/").setViewName("swagger");
            registry.addViewController("/discover").setViewName("swagger/discover");
            Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("views.properties"));
            for(Object key : properties.keySet()) {
                registry.addViewController((String)key).setViewName("/swagger/" + properties.getProperty((String) key));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not find view.properties",e);
        }
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

}
