package io.github.miracelwhipp.net.cs.plugin.registry;

import io.github.miracelwhipp.net.cs.plugin.BootstrapNuGetWagon;
import io.github.miracelwhipp.net.cs.plugin.NuGetBootstrapDownloader;
import io.github.miracelwhipp.net.nuget.plugin.NugetArtifact;
import io.github.miracelwhipp.net.provider.FrameworkVersion;
import io.github.miracelwhipp.net.provider.NetFrameworkProvider;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.io.File;
import java.io.IOException;

/**
 * This {@link NetFrameworkProvider} downloads the framework file directly using the nuget download manager
 * also used by the maven dependency mechanism. It uses a fixed version. sub classes may override the version
 *
 * @author miracelwhipp
 */
@Component(role = NetFrameworkProvider.class, hint = "fixed", instantiationStrategy = "singleton")
public class FixedVersionNugetDownloadNetFrameworkProvider implements NetFrameworkProvider {

	public static final FrameworkVersion DEFAULT_FRAMEWORK_VERSION =
			FrameworkVersion.newInstance(".NETStandard", "netstandard", 2, 0, 3);

	private final FrameworkVersion frameworkVersion;

	@Requirement
	private BootstrapNuGetWagon wagon;

	@Requirement
	private MavenSession session;

	public FixedVersionNugetDownloadNetFrameworkProvider() {
		this(DEFAULT_FRAMEWORK_VERSION);
	}

	public FixedVersionNugetDownloadNetFrameworkProvider(FrameworkVersion frameworkVersion) {
		this.frameworkVersion = frameworkVersion;
	}

	@Override
	public File getFrameworkLibrary(String name) throws IOException {

		NugetArtifact artifact = NugetArtifact.newInstance(
				"NETStandard.Library", name, getFrameworkVersion().mavenVersion(), "", "dll");

		return NuGetBootstrapDownloader.get(wagon, session, artifact);
	}

	@Override
	public FrameworkVersion getFrameworkVersion() {

		return frameworkVersion;
	}
}
