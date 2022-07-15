/*
 * Copyright 2022 freya022, and the jdaction-maven-plugin contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freya02.jdaction.maven;

import com.freya02.jdaction.JDAction;
import com.freya02.jdaction.NoActionClassVisitor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

// Would have been more intuitive to use PROCESS_CLASSES but this doesn't work on `compile`
// Compile here still works for us as maven compiles first, then executes this plugin, lol
@Mojo(name = "jdaction-check", defaultPhase = LifecyclePhase.COMPILE)
public class EnforcerMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "false")
	boolean ignoreFailures;

	private final HashSet<String> sourceWithIssues = new HashSet<>();

	@Override
	public void execute() throws MojoFailureException {
		try {
			if (ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(s -> s.startsWith("-agentlib:jdwp"))) {
				System.out.println("Type for debug");
				new Scanner(System.in).nextLine();
			}

			final Set<String> classpath = new HashSet<>();
			classpath.addAll(project.getCompileClasspathElements());
			classpath.addAll(project.getTestClasspathElements());
			classpath.addAll(project.getRuntimeClasspathElements());

			int totalIssues = 0;
			for (String target : classpath) {
				final Path rootPath = Paths.get(target);
				if (Files.notExists(rootPath)) continue;

				try (Stream<Path> stream = Files.walk(rootPath)) {
					totalIssues += stream
							.filter(p -> p.getFileName().toString().endsWith(".class"))
							.mapToInt(path -> {
								try {
									return inspectClass(path);
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							})
							.sum();
				}
			}

			for (String sourceWithIssue : sourceWithIssues) {
				doLog(sourceWithIssue + ":0 Please see https://jda.wiki/using-jda/using-restaction/ for more details");
			}

			if (totalIssues > 0 && !ignoreFailures) {
				throw new MojoFailureException("There are rest actions not being executed, please check errors above.");
			}
		} catch (MojoFailureException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoFailureException("Failed to check RestAction usages", e);
		}
	}

	private int inspectClass(Path path) throws IOException {
		final NoActionClassVisitor visitor = JDAction.inspectPath(path);

		if (visitor.getIssueCount() > 0) {
			sourceWithIssues.add(visitor.getSimpleSourceFile());
		}

		for (NoActionClassVisitor.NoActionIssue issue : visitor.getIssues().values()) {
			doLog(issue.getAsMessage(ignoreFailures)); //Don't use format for warnings
		}

		return visitor.getIssueCount();
	}

	private void doLog(String message) {
		if (ignoreFailures) {
			getLog().warn(message);
		} else {
			getLog().error(message);
		}
	}
}
