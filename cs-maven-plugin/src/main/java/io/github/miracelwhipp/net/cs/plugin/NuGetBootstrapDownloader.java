package io.github.miracelwhipp.net.cs.plugin;

import io.github.miracelwhipp.net.nuget.plugin.NugetArtifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * This utility class helps downloading nuget artifacts directly where using the maven dependency mechanism would
 * provide bootstrapping issues.
 *
 * @author miracelwhipp
 */
public final class NuGetBootstrapDownloader {

	private NuGetBootstrapDownloader() {
	}

	public static synchronized File get(Wagon wagon, MavenSession session, NugetArtifact artifact) throws IOException {

		try {

			String repositoryBaseDirectory = session.getLocalRepository().getBasedir();

			File targetFile = new File(repositoryBaseDirectory);

			targetFile = new File(targetFile, artifact.getWagonArtifact().getArtifactFilename().getPath());

			if (targetFile.exists()) {

				wagon.getIfNewer(artifact.getWagonArtifact().mavenResourceString(),
						targetFile, Files.getLastModifiedTime(targetFile.toPath()).toMillis());

			} else {

				wagon.get(artifact.getWagonArtifact().mavenResourceString(), targetFile);
			}

			return targetFile;

		} catch (TransferFailedException | ResourceDoesNotExistException | AuthorizationException e) {

			throw new IOException("unable to download " + artifact.getWagonArtifact().mavenResourceString(), e);
		}
	}

}
