package org.lazydoc.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.lazydoc.config.Config;
import org.lazydoc.LazyDoc;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "document", defaultPhase = LifecyclePhase.COMPILE, executionStrategy = "always")
@Execute(goal = "document", phase = LifecyclePhase.COMPILE)
public class LazyDocMojo extends AbstractMojo {


    @Parameter
    private Config config;

    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Using classpath: "+ StringUtils.join(classpath, ","));
        log.info(config.toString());
        try {

            List<URL> classpathUrls = new ArrayList<>();
            for(String classpathUrl : classpath) {
                classpathUrls.add(new File(classpathUrl).toURI().toURL());
            }
            ClassLoader classLoader = URLClassLoader.newInstance(classpathUrls.toArray(new URL[classpathUrls.size()]), Thread.currentThread().getContextClassLoader() );
            Thread.currentThread().setContextClassLoader(classLoader);

            new LazyDoc(config).document();
        } catch (Exception e) {
            getLog().error("Error parsing for documentation.", e);
            throw new MojoFailureException("Error parsing for documentation."+e.getMessage());
        }
    }
}
