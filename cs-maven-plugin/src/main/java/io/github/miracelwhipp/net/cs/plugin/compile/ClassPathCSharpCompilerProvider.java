package io.github.miracelwhipp.net.cs.plugin.compile;

import io.github.miracelwhipp.net.common.Streams;
import io.github.miracelwhipp.net.provider.CSharpCompilerProvider;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;
import java.io.IOException;

/**
 * This {@link CSharpCompilerProvider} downloads the c# compiler by the maven dependency mechanism.
 *
 * @author miracelwhipp
 */
@Component(role = CSharpCompilerProvider.class, hint = "default", instantiationStrategy = "singleton")
public class ClassPathCSharpCompilerProvider implements CSharpCompilerProvider {

	private File csharpCompiler;

	@Override
	public synchronized File getCSharpCompiler() throws IOException {

		if (csharpCompiler == null) {

			csharpCompiler = Streams.getResourceFile(getClass(), "tools", "csc", "exe");
		}

		return csharpCompiler;
	}
}
