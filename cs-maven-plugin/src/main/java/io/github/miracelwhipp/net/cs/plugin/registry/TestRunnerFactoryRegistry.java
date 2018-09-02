package io.github.miracelwhipp.net.cs.plugin.registry;

import io.github.miracelwhipp.net.provider.NetTestRunnerFactory;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.Map;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
@Component(role = TestRunnerFactoryRegistry.class)
public class TestRunnerFactoryRegistry {

	@Requirement(role = NetTestRunnerFactory.class)
	private Map<String, NetTestRunnerFactory> factories;

	public NetTestRunnerFactory getFactory() {

		return factories.get("xunit");
	}

}
