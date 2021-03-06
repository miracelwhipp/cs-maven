package io.github.miracelwhipp.net.cs.plugin;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * This goal generates an AssemblyInfo.cs source file with assembly information.
 *
 * @author jschwarz
 */
@Mojo(
		name = "generate-assembly-info",
		defaultPhase = LifecyclePhase.GENERATE_SOURCES,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class GenerateAssemblyInfo extends AbstractNetMojo {

	private static final String FILE_NAME = "AssemblyInfo.cs";
	public static final int BUFFER_SIZE = 512 * 1024;
	public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

	@Parameter(defaultValue = "false", property = "cs.assembly.info.skip")
	private boolean skip;

	/**
	 * This parameter specifies a description for an assembly.
	 */
	@Parameter(defaultValue = "${project.name}", property = "cs.assembly.title")
	private String title;

	/**
	 * This parameter provides a text description for an assembly.
	 */
	@Parameter(defaultValue = "${project.description}", property = "cs.assembly.description")
	private String description;

	/**
	 * This parameters specifies the build configuration, such as retail or debug, for an assembly.
	 */
	@Parameter(property = "cs.assembly.configuration")
	private String configuration;

	/**
	 * This parameter defines a company name for the assembly manifest.
	 */
	@Parameter(property = "cs.assembly.company")
	private String company;

	/**
	 * This parameter defines a product name for the assembly manifest.
	 */
	@Parameter(defaultValue = "${project.name}", property = "cs.assembly.product")
	private String product;

	/**
	 * This parameter defines a copyright for the assembly manifest.
	 */
	@Parameter(property = "cs.assembly.copyright")
	private String copyright;

	/**
	 * This parameter defines a trademark for the assembly manifest.
	 */
	@Parameter(property = "cs.assembly.trademark")
	private String trademark;

	/**
	 * This parameter specifies which culture the assembly supports.
	 */
	@Parameter(property = "cs.assembly.culture")
	private String culture;

	/**
	 * This parameter defines the ID of the typelib if this project is exposed to COM.
	 */
	@Parameter(property = "cs.assembly.guid")
	private String guid;

	/**
	 * This parameter defines the version information of the assembly. It consists of the following four values:
	 * <p>
	 * Major Version
	 * Minor Version
	 * Build Number
	 * Revision
	 * <p>
	 * You can specify all the values or you can default the Build and Revision Numbers
	 * by using the '*' as shown below:
	 * 1.0.*
	 */
	@Parameter(defaultValue = "${project.version}", property = "cs.assembly.version")
	private String version;

	/**
	 * This parameter instructs the compiler to use a specific version number for the Win32 file version resource.
	 * The Win32 file version is not required to be the same as the assembly's version number.
	 */
	@Parameter(defaultValue = "${project.version}", property = "cs.assembly.file.version")
	private String fileVersion;

	@Parameter(readonly = true, defaultValue = "${project}")
	protected MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip) {

			return;
		}

		String classifier = getClassifier();

		if (title == null) {

			title = project.getArtifactId();
		}

		if (product == null) {

			product = project.getArtifactId();
		}

		if (guid == null) {

			guid = UUID.nameUUIDFromBytes((
					project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion() + classifier
			).getBytes(StandardCharsets.UTF_8)).toString();
		}

		if (version.endsWith(SNAPSHOT_SUFFIX)) {

			version = version.substring(0, version.length() - SNAPSHOT_SUFFIX.length());
		}

		if (fileVersion.endsWith(SNAPSHOT_SUFFIX)) {

			fileVersion = fileVersion.substring(0, fileVersion.length() - SNAPSHOT_SUFFIX.length());
		}

		if (copyright == null && company != null) {

			copyright = "Copyright © " + company;
		}

		if (copyright == null) {

			copyright = "";
		}

		if (company == null) {

			company = "";
		}

		if (culture == null) {

			culture = "";
		}

		if (trademark == null) {

			trademark = "";
		}

		if (configuration == null) {

			configuration = "";
		}

		if (description == null) {

			description = "";
		}

		File generatedSourceDirectory = getGeneratedSourceDirectory();

		if (!generatedSourceDirectory.exists()) {

			if (!generatedSourceDirectory.mkdirs()) {

				throw new MojoExecutionException("unable to create directory " + generatedSourceDirectory);
			}

		} else {

			if (!generatedSourceDirectory.isDirectory()) {

				throw new MojoExecutionException("generated sources directory is a file");
			}
		}

		try (
				InputStream source = GenerateAssemblyInfo.class.getClassLoader().
						getResourceAsStream("assembly-info-template.cs");
				ByteArrayOutputStream memory = new ByteArrayOutputStream();
				OutputStream target = new FileOutputStream(new File(generatedSourceDirectory, FILE_NAME))
		) {

			int read;
			int length = 0;
			byte[] buffer = new byte[BUFFER_SIZE];

			while ((read = source.read(buffer)) >= 0) {

				memory.write(buffer, 0, read);
				length += read;
			}

			String assemblyInfo = new String(buffer, 0, length, StandardCharsets.UTF_8);

			assemblyInfo = assemblyInfo.replace("${assembly.title}", StringEscapeUtils.escapeJava(title));
			assemblyInfo = assemblyInfo.replace("${assembly.description}", StringEscapeUtils.escapeJava(description));
			assemblyInfo = assemblyInfo.replace("${assembly.configuration}", StringEscapeUtils.escapeJava(configuration));
			assemblyInfo = assemblyInfo.replace("${assembly.company}", StringEscapeUtils.escapeJava(company));
			assemblyInfo = assemblyInfo.replace("${assembly.product}", StringEscapeUtils.escapeJava(product));
			assemblyInfo = assemblyInfo.replace("${assembly.copyright}", StringEscapeUtils.escapeJava(copyright));
			assemblyInfo = assemblyInfo.replace("${assembly.trademark}", StringEscapeUtils.escapeJava(trademark));
			assemblyInfo = assemblyInfo.replace("${assembly.culture}", StringEscapeUtils.escapeJava(culture));
			assemblyInfo = assemblyInfo.replace("${assembly.com.visible}", Boolean.toString(comVisible));
			assemblyInfo = assemblyInfo.replace("${assembly.guid}", StringEscapeUtils.escapeJava(guid));
			assemblyInfo = assemblyInfo.replace("${assembly.version}", version);
			assemblyInfo = assemblyInfo.replace("${assembly.file.version}", fileVersion);

			target.write(assemblyInfo.getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {

			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
