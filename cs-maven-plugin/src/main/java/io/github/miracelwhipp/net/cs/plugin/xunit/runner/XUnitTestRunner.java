package io.github.miracelwhipp.net.cs.plugin.xunit.runner;

import io.github.miracelwhipp.net.common.Streams;
import io.github.miracelwhipp.net.common.Xml;
import io.github.miracelwhipp.net.cs.plugin.BootstrapNuGetWagon;
import io.github.miracelwhipp.net.cs.plugin.NuGetBootstrapDownloader;
import io.github.miracelwhipp.net.nuget.plugin.NugetArtifact;
import io.github.miracelwhipp.net.provider.NetTestRunner;
import io.github.miracelwhipp.net.provider.TestExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
public class XUnitTestRunner implements NetTestRunner {

	private static final NugetArtifact COMPILER_CONTAINER_ARTIFACT = NugetArtifact.newInstance(
			"xunit.runner.console",
			"xunit.runner.console",
			"2.4.0",
			"",
			"nupkg"
	);


	public static final String NET_472_SUB_DIRECTORY = "tools/net472";

	private final BootstrapNuGetWagon wagon;
	private final MavenSession session;
	private final File runtimeWrapperExecutable;
	private final File workingDirectory;
	private final Logger logger;


	public XUnitTestRunner(BootstrapNuGetWagon wagon, MavenSession session, File runtimeWrapperExecutable, File workingDirectory, Logger logger) {
		this.wagon = wagon;
		this.session = session;
		this.runtimeWrapperExecutable = runtimeWrapperExecutable;
		this.workingDirectory = workingDirectory;
		this.logger = logger;
	}

	@Override
	public void runTests(File testLibrary, List<String> includes, List<String> excludes, File resultFile) throws TestExecutionException {

		try {

			File completeResultFile = new File(resultFile.getParent(), "all-tests.xml");

			extractRunnerFiles(testLibrary.getParentFile());

			ProcessBuilder builder = new ProcessBuilder();

			builder.directory(workingDirectory);

			if (runtimeWrapperExecutable != null) {
				builder.command().add(runtimeWrapperExecutable.getAbsolutePath());
			}

			builder.command().add(new File(testLibrary.getParentFile(), "xunit.console.exe").getAbsolutePath());

			builder.command().add(testLibrary.getAbsolutePath());

			for (String include : includes) {

				builder.command().add("-class");
				builder.command().add(include);
			}

			for (String exclude : excludes) {

				builder.command().add("-noclass");
				builder.command().add(exclude);
			}

			builder.command().add("-junit");
			builder.command().add(completeResultFile.getAbsolutePath());

			builder.inheritIO();

			Process process = builder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue != 0) {

				logger.debug("xUnit runner finished with exit value " + exitValue);
				throw new TestExecutionException("xunit runner failed with exit value " + exitValue);
			}

			if (!completeResultFile.getAbsoluteFile().exists()) {

				throw new TestExecutionException("result file was not generated. file = " + completeResultFile.getAbsolutePath());
			}

			try (InputStream styleSheet = XUnitTestRunner.class.getClassLoader().getResourceAsStream("generate-result.xsl")) {

				Xml.transformFile(completeResultFile, new StreamSource(styleSheet), resultFile);
			}

		} catch (InterruptedException | IOException | ParserConfigurationException | SAXException | TransformerException e) {

			throw new TestExecutionException(e);
		}
	}

	private void extractRunnerFiles(File parentFile) throws TestExecutionException {

		try {

			File container = NuGetBootstrapDownloader.get(wagon, session, COMPILER_CONTAINER_ARTIFACT);

			link(parentFile, container, "xunit.console.exe");
			link(parentFile, container, "xunit.abstractions.dll");
			link(parentFile, container, "xunit.runner.reporters.net452.dll");
			link(parentFile, container, "xunit.runner.utility.net452.dll");

		} catch (IOException e) {

			throw new TestExecutionException(e);
		}

	}

	private void link(File parentFile, File container, String filename) throws IOException {

		File file = Streams.unpackForFile(container, new File(NET_472_SUB_DIRECTORY, filename));

		Files.createLink(new File(parentFile, file.getName()).toPath(), file.toPath());
	}
}
