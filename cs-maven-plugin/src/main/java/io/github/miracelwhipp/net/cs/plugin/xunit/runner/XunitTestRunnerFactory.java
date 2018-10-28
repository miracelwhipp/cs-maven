package io.github.miracelwhipp.net.cs.plugin.xunit.runner;

import io.github.miracelwhipp.net.cs.plugin.BootstrapNuGetWagon;
import io.github.miracelwhipp.net.provider.NetTestRunner;
import io.github.miracelwhipp.net.provider.NetTestRunnerFactory;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
@Component(role = NetTestRunnerFactory.class, hint = "xunit")
public class XunitTestRunnerFactory implements NetTestRunnerFactory {

	@Requirement
	private Logger logger;

	@Requirement
	private BootstrapNuGetWagon wagon;

	@Requirement
	private MavenSession session;


	@Override
	public NetTestRunner newRunnerForDirectory(File workingDirectory, File runtimeWrapperExecutable) {
		return new XUnitTestRunner(wagon, session, runtimeWrapperExecutable, workingDirectory, logger);
	}
}
