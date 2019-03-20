package io.github.miracelwhipp.net.cs.plugin.registry;

import io.github.miracelwhipp.net.common.Streams;
import io.github.miracelwhipp.net.common.Xml;
import io.github.miracelwhipp.net.provider.FrameworkVersion;
import io.github.miracelwhipp.net.provider.NetFrameworkProvider;
import org.codehaus.plexus.component.annotations.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
//@Component(role = NetFrameworkProvider.class, hint = "class-path", instantiationStrategy = "singleton")
public class ClassPathNetFrameworkProvider implements NetFrameworkProvider {

	private FrameworkVersion version = null;

	@Override
	public File getFrameworkLibrary(String name) throws IOException {

		return Streams.getResourceFile(
				ClassPathNetFrameworkProvider.class, "build/" + getDefaultFrameworkVersion().versionedToken() + "/ref", name, "dll");
	}

	@Override
	public FrameworkVersion getDefaultFrameworkVersion() {

		try {

			if (version != null) {

				return version;
			}

			File resourceFile =
					Streams.getResourceFile(getClass(), null, "NETStandard.Library", "nuspec");

			if (resourceFile == null || !resourceFile.exists()) {

				return FrameworkVersion.defaultVersion();
			}

			Document nuSpec = Xml.parse(resourceFile, false);

			final String versionString = Xml.evaluateXpath(nuSpec, "/package/metadata/version");

			version = FrameworkVersion.fromShortName("netstandard" + versionString);

			return version;

		} catch (SAXException | XPathExpressionException | ParserConfigurationException | IOException e) {

			throw new IllegalStateException(e);
		}
	}
}
