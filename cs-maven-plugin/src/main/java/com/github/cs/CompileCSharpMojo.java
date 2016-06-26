package com.github.cs;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This goal compiles the c# sources and creates the projects artifact.
 *
 * @author miracelwhipp
 */
@Mojo(
		name = "compile",
		defaultPhase = LifecyclePhase.COMPILE,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CompileCSharpMojo extends AbstractCompileCSharpMojo {

	@Parameter(defaultValue = "${project.basedir}/src/main/cs", property = "cs.source.directory")
	private File csSourceDirectory;

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	private File workingDirectory;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}")
	private String outputFile;

	private static final Set<String> ALLOWED_SCOPES =
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList("compile", "provided", "system")));

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			String target = project.getArtifact().getArtifactHandler().getPackaging();

			CSharpCompilerTargetType targetType = CSharpCompilerTargetType.fromString(target);

			if (targetType == null) {

				throw new MojoFailureException("unknown target type : " + target);
			}

			File assembly = compile(
					workingDirectory,
					csSourceDirectory,
					outputFile,
					targetType,
					ALLOWED_SCOPES,
					preprocessorDefines
			);

			if (assembly == null) {

				throw new MojoFailureException("There were no main sources to compile.");
			}

			project.getArtifact().setFile(assembly);

		} catch (DependencyResolutionException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}

	}

}
