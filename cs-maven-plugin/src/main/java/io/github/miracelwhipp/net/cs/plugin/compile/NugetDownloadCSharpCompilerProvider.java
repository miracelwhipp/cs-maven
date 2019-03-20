package io.github.miracelwhipp.net.cs.plugin.compile;

import io.github.miracelwhipp.net.common.Streams;
import io.github.miracelwhipp.net.cs.plugin.BootstrapNuGetWagon;
import io.github.miracelwhipp.net.cs.plugin.NuGetBootstrapDownloader;
import io.github.miracelwhipp.net.nuget.plugin.NugetArtifact;
import io.github.miracelwhipp.net.provider.CSharpCompilerProvider;
import org.apache.maven.execution.MavenSession;

import java.io.File;
import java.io.IOException;

/**
 * This {@link CSharpCompilerProvider} downloads the c# compiler directly using the nuget download manager
 * also used by the maven dependency mechanism.
 *
 * @author miracelwhipp
 */
//@Component(role = CSharpCompilerProvider.class, hint = "default", instantiationStrategy = "singleton")
public class NugetDownloadCSharpCompilerProvider implements CSharpCompilerProvider {

	private static final NugetArtifact COMPILER_CONTAINER_ARTIFACT = NugetArtifact.newInstance(
			"microsoft.net.compilers",
			"microsoft.net.compilers",
			"2.8.0",
			"",
			"nupkg"
	);

	private static final File COMPILER_FILE = new File("tools/csc.exe");

	private final NugetArtifact compilerContainerArtifact;
	private final File compilerFile;
	private final BootstrapNuGetWagon wagon;
	private final MavenSession session;

	private File result;

	public NugetDownloadCSharpCompilerProvider(BootstrapNuGetWagon wagon, MavenSession session) {
		compilerContainerArtifact = COMPILER_CONTAINER_ARTIFACT;
		compilerFile = COMPILER_FILE;
		this.wagon = wagon;
		this.session = session;
	}

	public NugetDownloadCSharpCompilerProvider(BootstrapNuGetWagon wagon, MavenSession session, String compilerVersion) {
		compilerContainerArtifact = NugetArtifact.newInstance(
				"microsoft.net.compilers",
				"microsoft.net.compilers",
				compilerVersion,
				"",
				"nupkg"
		);
		compilerFile = COMPILER_FILE;
		this.wagon = wagon;
		this.session = session;
	}

	@Override
	public synchronized File getCSharpCompiler() throws IOException {

		if (result != null) {

			return result;
		}

		File container = NuGetBootstrapDownloader.get(wagon, session, compilerContainerArtifact);

		return result = Streams.unpackForFile(container, compilerFile);
	}
}
