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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.lazydoc.config.Config;
import org.lazydoc.config.PrinterConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@Mojo(name = "document", defaultPhase = LifecyclePhase.COMPILE, executionStrategy = "always")
@Execute(goal = "document", phase = LifecyclePhase.COMPILE)
public class LazyDocMojo extends AbstractMojo {


    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArtifactFactory artifactFactory;

    @Component
    private ArtifactMetadataSource metadataSource;

    @Parameter(readonly = true, required = true, defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    @Parameter(readonly = true, required = true, defaultValue = "${project.remoteArtifactRepositories}")
    private List<ArtifactRepository> remoteRepositories;

    @Component
    private MavenProjectBuilder projectBuilder;

    @Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
    private List<Artifact> pluginDependencies;

    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpath;

    @Parameter
    private Config config;

    @Parameter
    private List<PrinterConfig> printerConfigs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        String logLevel = log.isDebugEnabled() ? "DEBUG" : log.isWarnEnabled() ? "WARN" : log.isInfoEnabled() ? "INFO" : "ERROR";
        log.info("Log level is "+logLevel);
        log.info(config.toString());
        try {
            ClassLoader classLoader = getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> lazyDocClass = classLoader.loadClass("org.lazydoc.LazyDoc");
            Class<?> lazyDocConfigClass = classLoader.loadClass("org.lazydoc.config.Config");
            Class<?> lazyDocPrinterConfigClass = classLoader.loadClass("org.lazydoc.config.PrinterConfig");
            Object lazydocConfig = lazyDocConfigClass.newInstance();
            BeanUtils.copyProperties(lazydocConfig, config);
            List lazyDocPrinterConfigs = new ArrayList();
            if (printerConfigs != null) {
                for(PrinterConfig printerConfig : printerConfigs) {
                    Object lazydocPrinterConfig = lazyDocPrinterConfigClass.newInstance();
                    BeanUtils.copyProperties(lazydocPrinterConfig, printerConfig);
                    lazyDocPrinterConfigs.add(lazydocPrinterConfig);
                }
            }
            lazyDocClass.getDeclaredMethod("document", lazyDocConfigClass, List.class, String.class).invoke(lazyDocClass.newInstance(), lazydocConfig, lazyDocPrinterConfigs, logLevel);
        } catch (Exception e) {
            getLog().error("Error parsing for documentation.", e);
            throw new MojoFailureException("Error parsing for documentation." + e.getMessage());
        }
    }

    private ClassLoader getClassLoader()
            throws MojoExecutionException, DependencyResolutionRequiredException {
        List<URL> classpathURLs = new ArrayList<URL>();
        this.addRelevantPluginDependenciesToClasspath(classpathURLs);
        this.addRelevantProjectDependenciesToClasspath(classpathURLs);
        for (URL classpath : classpathURLs) {
            getLog().info("Classpath: " + classpath.toString());
        }
        return new URLClassLoader(classpathURLs.toArray(new URL[classpathURLs.size()]));
    }

    private void addRelevantPluginDependenciesToClasspath(List<URL> path)
            throws MojoExecutionException {
        try {
            for (Artifact classPathElement : new HashSet<Artifact>(this.pluginDependencies)) {
                URL url = classPathElement.getFile().toURI().toURL();
                getLog().debug("Adding plugin dependency artifact: " + classPathElement.getArtifactId()
                        + " to classpath ("+url+")");
                path.add(url);
            }
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error during setting up classpath", e);
        }

    }


    private void addRelevantProjectDependenciesToClasspath(List<URL> path)
            throws MojoExecutionException, DependencyResolutionRequiredException {
        try {
            getLog().debug("Project Dependencies will be included.");

            List<Artifact> artifacts = new ArrayList<Artifact>();
            List<File> theClasspathFiles = new ArrayList<File>();

            collectProjectArtifactsAndClasspath(artifacts, theClasspathFiles);

            for (File classpathFile : theClasspathFiles) {
                URL url = classpathFile.toURI().toURL();
                getLog().debug("Adding to classpath : " + url);
                path.add(url);
            }

            for (Artifact classPathElement : artifacts) {
                getLog().debug("Artifact: "+classPathElement);
                getLog().debug("Artifact file: "+classPathElement.getFile());
                URL url = classPathElement.getFile().toURI().toURL();
                getLog().debug("Adding project dependency artifact: " + classPathElement.getArtifactId()
                        + " to classpath ("+url+")");
                path.add(url);
            }

        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error during setting up classpath", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void collectProjectArtifactsAndClasspath(List<Artifact> artifacts, List<File> theClasspathFiles) throws MojoExecutionException, DependencyResolutionRequiredException {
        artifacts.addAll(project.getCompileDependencies());
        artifacts.addAll(resolveProjectDependencies(project.getDependencies()));
        theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
        getLog().debug("Collected project artifacts " + artifacts);
        getLog().debug("Collected project classpath " + theClasspathFiles);
    }

    private Set<Artifact> resolveProjectDependencies(List<Dependency> dependencies) throws MojoExecutionException {
        Set<Artifact> resolvedArtifacts = new HashSet<>();
        try {
            getLog().debug("Project dependencies: "+dependencies);
            // make Artifacts of all the dependencies
            Set<Artifact> dependencyArtifacts = MavenMetadataSource.createArtifacts(this.artifactFactory, dependencies, null, null, null);
            getLog().debug("Artifacts build from dependencies: "+dependencyArtifacts);

            for (Artifact dependencyArtifact : dependencyArtifacts) {
                artifactResolver.resolve(dependencyArtifact, this.remoteRepositories, this.localRepository);
                ArtifactResolutionResult result = artifactResolver.resolveTransitively(dependencyArtifacts, dependencyArtifact, this.remoteRepositories, this.localRepository, this.metadataSource);
                resolvedArtifacts.addAll(result.getArtifacts());
            }
            resolvedArtifacts.addAll(dependencyArtifacts);
            return resolvedArtifacts;
        } catch (Exception ex) {
            throw new MojoExecutionException("Encountered problems resolving dependencies of the executable "
                    + "in preparation for its execution.", ex);
        }
    }

}
