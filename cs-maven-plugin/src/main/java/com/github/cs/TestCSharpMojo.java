package com.github.cs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;
import org.eclipse.aether.graph.Dependency;
import org.xml.sax.SAXException;

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

	private static final String TEST_RESULT_FILE = "TestResult.xml";
	public static final String TRANSFORMATION_FILE = "nunit-to-junit.xsl";
	public static final int BUFFER_SIZE = 128 * 1024;


	@Parameter(readonly = true, defaultValue = "${project.build.testOutputDirectory}")
	private File testOutputDirectory;

	@Parameter(defaultValue = "${project.build.testOutputDirectory}")
	private File testWorkingDirectory;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}-tests.dll")
	private String testLibrary;

	@Parameter
	private List<String> includes;

	@Parameter
	private List<String> excludes;

	@Parameter(defaultValue = "false", property = "skipTests")
	private boolean skipTests;

	@Parameter(defaultValue = "${project.build.directory}/nunit-reports")
	private File reportsDirectory;

	@Parameter(defaultValue = "false", property = "maven.test.failure.ignore")
	private boolean ignoreFailure;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}")
	private String outputFile;

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	private File targetDirectory;

	@Parameter
	private Map<String, String> additionalVariables = new HashMap<>();

	@Parameter(defaultValue = "false")
	private boolean executeTests32bit;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			if (skipTests) {

				getLog().info("The tests are skipped.");

				return;
			}

			String targetString = project.getArtifact().getArtifactHandler().getPackaging();

			CSharpCompilerTargetType targetType = getTargetType();

			if (targetType == null) {

				throw new MojoFailureException("unknown target type : " + targetString);
			}

			reportsDirectory.mkdirs();

			try (
					InputStream source =
							TestCSharpMojo.class.getClassLoader().getResourceAsStream(TRANSFORMATION_FILE);
					FileOutputStream target = new FileOutputStream(new File(reportsDirectory, TRANSFORMATION_FILE))
			) {

				int length = 0;
				byte[] buffer = new byte[BUFFER_SIZE];

				while ((length = source.read(buffer)) > 0) {

					target.write(buffer, 0, length);
				}
			}


			File testLibraryFile = new File(testOutputDirectory, testLibrary);

			if (!testLibraryFile.exists()) {

				getLog().info("Test library file (" + testLibraryFile.getAbsolutePath() + ") does not exist. Tests execution skipped.");
				return;
			}

			if (!testWorkingDirectory.equals(testOutputDirectory)) {

				copyToLib(testLibraryFile);
				testLibraryFile = new File(testWorkingDirectory, testLibrary);
			}


			String condition = buildCondition(deNullify(includes), deNullify(excludes));

			NetFrameworkProvider frameworkProvider = getFrameworkProvider();

			File runner = frameworkProvider.getNUnitRunner();

			ProcessBuilder builder = new ProcessBuilder(runner.getPath());
			builder.directory(testWorkingDirectory);
			builder.environment().putAll(additionalVariables);

			builder.command().add(testLibraryFile.getAbsolutePath());

			if (!condition.isEmpty()) {
				builder.command().add("--where");
				builder.command().add("\"" + condition + "\"");
			}

			builder.command().add("--result:" + new File(reportsDirectory, TEST_RESULT_FILE).getAbsolutePath());

			if (executeTests32bit) {

				builder.command().add("-x86");
			}

			copyToLib(frameworkProvider.getNUnitLibrary());

			copyToLib(new File(targetDirectory, outputFile + "." + targetType.getFileSuffix()));

			List<File> testDependencies = getTestDependencies();

			for (File testDependency : testDependencies) {

				copyToLib(testDependency);
			}

			builder.inheritIO();

			Process process = builder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue == -2) {
				// TODO: verify that this occurs if and only if the library does not contain tests
				getLog().info("No tests found.");
				return;
			}

			transformResultFile(exitValue);

			if (exitValue != 0) {

				getLog().debug("nUnit runner finished with exit value " + exitValue);

				if (!ignoreFailure) {
					throw new MojoFailureException("There were test failures.");
				}
			}


		} catch (IOException | InterruptedException | DependencyResolutionException e) {

			throw new MojoFailureException("unable to run tests", e);
		}


	}

	private void transformResultFile(final int exitValue) throws MojoFailureException {

		try {
			final File styleSheet = new File(reportsDirectory, TRANSFORMATION_FILE);
			XmlTransformation.transformFile(new File(reportsDirectory, TEST_RESULT_FILE), new StreamSource(styleSheet), new File(reportsDirectory, "result.xml"), new XmlTransformation.ParameterSetter() {
				@Override
				public void setParameters(Transformer transformer) {
					transformer.setParameter("target-directory", reportsDirectory.getAbsolutePath());
					transformer.setParameter("nunit-result", exitValue);
				}
			});
		} catch (TransformerException | ParserConfigurationException | SAXException | IOException e) {

			throw new MojoFailureException("unable to transform file.", e);
		}

	}

	private void copyToLib(File file) throws IOException {

		try (InputStream source = new FileInputStream(file);
		     OutputStream target = new FileOutputStream(new File(testWorkingDirectory, file.getName()))) {

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
