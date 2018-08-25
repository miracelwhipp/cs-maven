package com.github.cs.compile;

import com.github.cs.CSharpCompilerTargetType;

/**
 * This immutable holds information about an assembly to be generated by a c# compiler.
 *
 * @author miracelwhipp
 */
public class AssemblyFileProperties {

	private final CSharpCompilerTargetType targetType;
	private final String targetFileName;
	private final String platform;


	public AssemblyFileProperties(CSharpCompilerTargetType targetType, String targetFileName, String platform) {
		this.targetType = targetType;
		this.targetFileName = targetFileName;
		this.platform = platform;
	}

	public CSharpCompilerTargetType getTargetType() {
		return targetType;
	}

	public String getTargetFileName() {
		return targetFileName;
	}

	public String getPlatform() {
		return platform;
	}
}
