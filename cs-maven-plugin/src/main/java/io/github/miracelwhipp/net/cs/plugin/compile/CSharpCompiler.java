package io.github.miracelwhipp.net.cs.plugin.compile;

import io.github.miracelwhipp.net.common.DependencyProvider;
import io.github.miracelwhipp.net.provider.CSharpCompilerProvider;
import io.github.miracelwhipp.net.provider.NetFrameworkProvider;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class interfaces the command line to invoke the c# compiler.
 *
 * @author miracelwhipp
 */
public class CSharpCompiler {

	private final File runtimeWrapper;
	private final SourceFiles sourceFiles;
	private final CSharpCompilerOptions compilerOptions;
	private final AssemblyFileProperties assemblyFileProperties;

	private final Log logger;
	private final NetFrameworkProvider frameworkProvider;
	private final CSharpCompilerProvider compilerProvider;


	public CSharpCompiler(
			File runtimeWrapper,
			SourceFiles sourceFiles,
			CSharpCompilerOptions compilerOptions,
			AssemblyFileProperties assemblyFileProperties,
			Log logger,
			NetFrameworkProvider frameworkProvider,
			CSharpCompilerProvider compilerProvider
	) {
		this.runtimeWrapper = runtimeWrapper;
		this.sourceFiles = sourceFiles;
		this.compilerOptions = compilerOptions;
		this.assemblyFileProperties = assemblyFileProperties;
		this.logger = logger;
		this.frameworkProvider = frameworkProvider;
		this.compilerProvider = compilerProvider;
	}

	public File compile() throws MojoFailureException {

		try {

			File compilerExecutable = compilerProvider.getCSharpCompiler();

			ProcessBuilder processBuilder = runtimeWrapper == null ?
					new ProcessBuilder() :
					new ProcessBuilder(runtimeWrapper.getAbsolutePath());

			processBuilder.command().add(compilerExecutable.getPath());

			processBuilder.command().add("/nostdlib");
			processBuilder.command().add("/noconfig");
			processBuilder.command().add("/utf8output");

			if (compilerOptions.isUnsafe()) {
				processBuilder.command().add("/unsafe");
			}

			for (String define : compilerOptions.getDefines()) {
				processBuilder.command().add("/define:" + define);
			}

			if (assemblyFileProperties.getPlatform() != null) {
				processBuilder.command().add("/platform:" + assemblyFileProperties.getPlatform());
			}

			processBuilder.command().add("/target:" + assemblyFileProperties.getTargetType().getArgumentId());

			String outFileName = assemblyFileProperties.getTargetFileName() + "." +
					assemblyFileProperties.getTargetType().getFileSuffix();

			File targetFile = new File(sourceFiles.getWorkingDirectory(), outFileName).getAbsoluteFile();

			processBuilder.command().add("/out:" + outFileName);

			String responseFile = buildResponseFile();

			if (responseFile == null) {
				return null;
			}

			processBuilder.command().add("@" + responseFile);

			logger.debug("executing csc:");
			for (String arg : processBuilder.command()) {
				logger.debug(arg);
			}

			processBuilder.directory(sourceFiles.getWorkingDirectory());
			processBuilder.inheritIO();

			Process process = processBuilder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue != 0) {

				throw new MojoFailureException("c# compiler finished with exit value " + exitValue);
			}

			return targetFile;

		} catch (IOException | InterruptedException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}
	}

	private String buildResponseFile() throws IOException, MojoFailureException {

		StringBuilder responseFileContent = new StringBuilder();

		boolean sourceFilesExistent = false;

		for (File csSourceDirectory : sourceFiles.getCsSourceDirectories()) {

			if (!csSourceDirectory.exists()) {

				continue;
			}

			Collection<File> files = FileUtils.listFiles(csSourceDirectory, new String[]{"cs"}, true);

			for (File file : files) {

				responseFileContent.append(file.getAbsolutePath()).append("\n");
				sourceFilesExistent = true;
			}
		}

		if (!sourceFilesExistent) {

			return null;
		}

		List<String> frameworkLibraries = new ArrayList<>();

		if (sourceFiles.getFrameworkReferences() != null) {
			frameworkLibraries.addAll(sourceFiles.getFrameworkReferences());
		}

		for (String frameworkLibrary : frameworkLibraries) {

			File library = frameworkProvider.getFrameworkLibrary(frameworkLibrary);

			if (library == null) {

				throw new MojoFailureException("cannot find framework library " + frameworkLibrary);
			}

			library = DependencyProvider.provideFile(library, null, sourceFiles.getWorkingDirectory());

			responseFileContent.append("/reference:").append(library.getAbsolutePath()).append("\n");
		}

		for (File referenceFile : sourceFiles.getReferenceFiles()) {

			if (referenceFile.getName().endsWith(".netmodule") || referenceFile.getName().endsWith(".obj")) {

				responseFileContent.append("/addmodule:").append(referenceFile.getAbsolutePath()).append("\n");

			} else {

				responseFileContent.append("/reference:").append(referenceFile.getAbsolutePath()).append("\n");
			}
		}

		for (String resource : sourceFiles.getResources()) {
			responseFileContent.append("/resource:").append(resource).append("\n");
		}

		if (sourceFiles.getKeyFile() != null) {

			responseFileContent.append("/keyfile:").append(sourceFiles.getKeyFile().getAbsolutePath()).append("\n");
		}

		Path path = new File(sourceFiles.getWorkingDirectory(), "sources.rsp").toPath();

		Files.write(path,
				responseFileContent.toString().getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
		);

		return path.toString();
	}
}
