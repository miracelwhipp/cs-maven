package com.github.cs;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * This goal executes the test for the c# code with the n-unit-3 console runner.
 *
 * @author miracelwhipp
 */
@Mojo(
		name = "test",
		defaultPhase = LifecyclePhase.TEST,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class TestCSharpMojo extends AbstractNetMojo {

	public static final String ENV_PATH = "Path";

	private static final Set<String> ALLOWED_SCOPES =
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList("compile", "provided", "system", "test")));

	@Parameter(readonly = true, defaultValue = "${project.build.testOutputDirectory}")
	private File workingDirectory;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}-tests.dll")
	private String testLibrary;

	@Parameter
	private List<String> includes;

	@Parameter
	private List<String> excludes;

	@Parameter(defaultValue = "${project.build.directory}/nunit-reports")
	private File reportsDirectory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			File testLibraryFile = new File(workingDirectory, testLibrary);

			if (!testLibraryFile.exists()) {

				getLog().info("Test library file (" + testLibraryFile.getAbsolutePath() + ") does not exist. Tests execution skipped.");
				return;
			}

			String condition = buildCondition(deNullify(includes), deNullify(excludes));

			NetFrameworkProvider frameworkProvider = getFrameworkProvider();

			File runner = frameworkProvider.getNUnitRunner();

			ProcessBuilder builder = new ProcessBuilder(runner.getPath());
			builder.directory(workingDirectory);


			builder.command().add(testLibraryFile.getAbsolutePath());

			if (!condition.isEmpty()) {
				builder.command().add("--where");
				builder.command().add("\"" + condition + "\"");
			}

//			String pathVariable = System.getenv(ENV_PATH);
//
//			if (!pathVariable.isEmpty()) {
//
//				pathVariable += ";";
//			}
//
//			pathVariable += ".";
//			pathVariable += ";" + frameworkProvider.getNUnitLibrary().getParent();
//			pathVariable += ";" + frameworkProvider.getMsCoreLibrary().getParent();
//			pathVariable += ";" + frameworkProvider.getNUnitRunner().getParent();
//			pathVariable += ";" + workingDirectory;

			copyToLib(frameworkProvider.getNUnitLibrary());
//			copyToLib(frameworkProvider.getMsCoreLibrary());
//			copyToLib(frameworkProvider.getNUnitRunner());

			List<File> testDependencies = getTestDependencies();

			for (File testDependency : testDependencies) {

				copyToLib(testDependency);
			}

//			for (File testDependency : testDependencies) {
//
//				pathVariable += ";" + testDependency.getParent();
//			}
//
//			getLog().debug("setting path environment variable of sub process to " + pathVariable);
//			builder.environment().put(ENV_PATH, pathVariable);

			builder.inheritIO();

			Process process = builder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue == -2) {
				// TODO: verify that this occurs if and only if the library does not contain tests
				getLog().info("No tests found.");
				return;
			}

			if (exitValue != 0) {

				getLog().debug("nUnit runner finished with exit value " + exitValue);
				throw new MojoFailureException("There were test failures.");
			}


		} catch (IOException | InterruptedException | DependencyResolutionException e) {

			throw new MojoFailureException("unable to run tests", e);
		}


	}

	private void copyToLib(File file) throws IOException {

		try (InputStream source = new FileInputStream(file);
		     OutputStream target = new FileOutputStream(new File(workingDirectory, file.getName()))) {

			byte[] buffer = new byte[128 * 1024];

			int length;

			while ((length = source.read(buffer)) > 0) {

				target.write(buffer, 0, length);
			}
		}
	}

	private List<File> getTestDependencies() throws DependencyResolutionException {

		List<Dependency> dllDependencies = getDllDependencies(ALLOWED_SCOPES);

		List<File> artifactFiles = new ArrayList<>(dllDependencies.size());

		for (Dependency dependency : dllDependencies) {

			artifactFiles.add(dependency.getArtifact().getFile());
		}

		return artifactFiles;
	}

	private List<String> deNullify(List<String> list) {

		if (list == null) {

			return Collections.emptyList();
		}

		return list;
	}

	private String buildCondition(List<String> includes, List<String> excludes) {

		StringBuilder result = new StringBuilder();

		appendIncludes(includes, result);

		appendExcludes(excludes, result);

		return result.toString();
	}

	private void appendExcludes(List<String> excludes, StringBuilder result) {

		if (excludes.isEmpty()) {

			return;
		}

		boolean first = true;

		for (String exclude : excludes) {

			if (first) {
				first = false;
			} else {
				result.append(" and ");
			}

			result.append("test !~ '");
			result.append(exclude);
			result.append("'");
		}

	}

	private void appendIncludes(List<String> includes, StringBuilder result) {

		if (includes.isEmpty()) {
			return;
		}

		result.append("(");

		boolean first = true;

		for (String include : includes) {

			if (first) {
				first = false;
			} else {
				result.append(" or ");
			}

			result.append("test =~ '");
			result.append(include);
			result.append("'");
		}

		result.append(")");
	}

}
