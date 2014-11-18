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
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
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
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.lang.reflect.Method;
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
            Method addPrinterConfig = lazyDocConfigClass.getDeclaredMethod("addPrinterConfig", lazyDocPrinterConfigClass);
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

    /**
     * Set up a classloader for the execution of the main class.
     *
     * @return the classloader
     * @throws MojoExecutionException if a problem happens
     */
    private ClassLoader getClassLoader()
            throws MojoExecutionException {
        List<URL> classpathURLs = new ArrayList<URL>();
        this.addRelevantPluginDependenciesToClasspath(classpathURLs);
        this.addRelevantProjectDependenciesToClasspath(classpathURLs);
        for (URL classpath : classpathURLs) {
            getLog().info("Classpath: " + classpath.toString());
        }
        return new URLClassLoader(classpathURLs.toArray(new URL[classpathURLs.size()]));
    }

    /**
     * Add any relevant project dependencies to the classpath. Indirectly takes includePluginDependencies and
     * ExecutableDependency into consideration.
     *
     * @param path classpath of {@link java.net.URL} objects
     * @throws MojoExecutionException if a problem happens
     */
    private void addRelevantPluginDependenciesToClasspath(List<URL> path)
            throws MojoExecutionException {
        try {
            for (Artifact classPathElement : new HashSet<Artifact>(this.pluginDependencies)) {
                getLog().debug("Adding plugin dependency artifact: " + classPathElement.getArtifactId()
                        + " to classpath");
                path.add(classPathElement.getFile().toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error during setting up classpath", e);
        }

    }


    /**
     * Add any relevant project dependencies to the classpath. Takes includeProjectDependencies into consideration.
     *
     * @param path classpath of {@link java.net.URL} objects
     * @throws MojoExecutionException if a problem happens
     */
    private void addRelevantProjectDependenciesToClasspath(List<URL> path)
            throws MojoExecutionException {
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
                getLog().debug("Adding project dependency artifact: " + classPathElement.getArtifactId()
                        + " to classpath");
                path.add(classPathElement.getFile().toURI().toURL());
            }

        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error during setting up classpath", e);
        }
    }

    /**
     * Collects the project artifacts in the specified List and the project specific classpath (build output and build
     * test output) Files in the specified List, depending on the plugin classpathScope value.
     *
     * @param artifacts         the list where to collect the scope specific artifacts
     * @param theClasspathFiles the list where to collect the scope specific output directories
     */
    @SuppressWarnings("unchecked")
    protected void collectProjectArtifactsAndClasspath(List<Artifact> artifacts, List<File> theClasspathFiles) {
        artifacts.addAll(project.getCompileArtifacts());
        theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
        getLog().debug("Collected project artifacts " + artifacts);
        getLog().debug("Collected project classpath " + theClasspathFiles);
    }


    /**
     * Get the artifact which refers to the POM of the executable artifact.
     *
     * @param executableArtifact this artifact refers to the actual assembly.
     * @return an artifact which refers to the POM of the executable artifact.
     */
    private Artifact getExecutablePomArtifact(Artifact executableArtifact) {
        return this.artifactFactory.createBuildArtifact(executableArtifact.getGroupId(),
                executableArtifact.getArtifactId(),
                executableArtifact.getVersion(), "pom");
    }

    /**
     * Resolve the executable dependencies for the specified project
     *
     * @param executablePomArtifact the project's POM
     * @return a set of Artifacts
     * @throws MojoExecutionException if a failure happens
     */
    private Set<Artifact> resolveExecutableDependencies(Artifact executablePomArtifact)
            throws MojoExecutionException {

        Set<Artifact> executableDependencies;
        try {
            MavenProject executableProject =
                    this.projectBuilder.buildFromRepository(executablePomArtifact, this.remoteRepositories,
                            this.localRepository);

            // get all of the dependencies for the executable project
            List<Dependency> dependencies = executableProject.getDependencies();

            // make Artifacts of all the dependencies
            Set<Artifact> dependencyArtifacts =
                    MavenMetadataSource.createArtifacts(this.artifactFactory, dependencies, null, null, null);

            // not forgetting the Artifact of the project itself
            dependencyArtifacts.add(executableProject.getArtifact());

            // resolve all dependencies transitively to obtain a comprehensive list of assemblies
            ArtifactResolutionResult result =
                    artifactResolver.resolveTransitively(dependencyArtifacts, executablePomArtifact,
                            Collections.emptyMap(), this.localRepository,
                            this.remoteRepositories, metadataSource, null,
                            Collections.emptyList());
            executableDependencies = result.getArtifacts();
        } catch (Exception ex) {
            throw new MojoExecutionException("Encountered problems resolving dependencies of the executable "
                    + "in preparation for its execution.", ex);
        }

        return executableDependencies;
    }

}
