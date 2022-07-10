package com.freya02.jdaction.maven;

import com.freya02.jdaction.JDAction;
import com.freya02.jdaction.NoActionClassVisitor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

	@Override //TODO make sure this plugin is executed AFTER compilation, perhaps look at executed goals ?
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if (ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(s -> s.startsWith("-agentlib:jdwp"))) {
				System.out.println("Type for debug");
				new Scanner(System.in).nextLine();
			}

			final Set<String> classpath = new HashSet<>();
			classpath.addAll(project.getCompileClasspathElements());
			classpath.addAll(project.getTestClasspathElements());
			classpath.addAll(project.getRuntimeClasspathElements());

			int issues = 0;
			for (String target : classpath) {
				final Path rootPath = Paths.get(target);
				if (Files.notExists(rootPath)) continue;

				try (Stream<Path> stream = Files.walk(rootPath)) {
					issues += stream
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

			if (issues > 0) {
				throw new MojoFailureException("There are rest actions not being executed, please check errors above.");
			}
		} catch (MojoFailureException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoFailureException("Failed to check RestAction usages", e);
		}
	}

	private int inspectClass(Path path) throws IOException {
		final NoActionClassVisitor visitor = JDAction.inspectPath(getLog(), path, false);

		if (visitor.getIssueCount() > 0) {
			getLog().error(visitor.getSimpleSourceFile() + ":0 Please see https://jda.wiki/using-jda/using-restaction/ for more details");
		}

		return visitor.getIssueCount();
	}
}
