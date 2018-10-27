package io.github.miracelwhipp.net.cs.plugin;


import io.github.miracelwhipp.net.nuget.plugin.AbstractNugetWagon;
import io.github.miracelwhipp.net.nuget.plugin.NugetPackageDownloadManager;
import io.github.miracelwhipp.net.provider.NetFrameworkProvider;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * This {@link AbstractNugetWagon} is used only for downloading nuget dependencies of the cs-maven-plugin.
 * Using the maven dependency mechanism would produce bootstrapping issues. It's only use case is to
 * provide the functionality of wagon nuget downloads without interfering with the life-cycle of the
 * normal NuGetWagon. It hence derives from it without adding functionality, thus providing a different
 * component context.
 *
 * @author miracelwhipp
 */
@Component(role = BootstrapNuGetWagon.class, instantiationStrategy = "singleton")
public class BootstrapNuGetWagon extends AbstractNugetWagon {

	@Requirement(optional = true)
	private NetFrameworkProvider frameworkProvider;

	@Requirement(hint = "https")
	private Wagon delegate;

	@Requirement
	private NugetPackageDownloadManager downloadManager;

	private boolean connected = false;

	@Override
	public Wagon getDelegate() {

		if (connected) {

			return delegate;
		}

		synchronized (this) {

			if (connected) {

				return delegate;
			}

			try {

				delegate.connect(new Repository("bootstrap-nuget", "https://api.nuget.org/v3-flatcontainer/"));
				connected = true;

			} catch (ConnectionException | AuthenticationException e) {

				throw new RuntimeException(e);
			}
		}

		return delegate;

//		return null;
	}

	@Override
	public NugetPackageDownloadManager getDownloadManager() {
		return downloadManager;

//		return null;
	}

	@Override
	public NetFrameworkProvider getFrameworkProvider() {

		return frameworkProvider;

//		return null;
	}
}
