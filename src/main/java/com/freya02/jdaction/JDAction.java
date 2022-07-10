package com.freya02.jdaction;

import org.apache.maven.plugin.logging.Log;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JDAction {
	public static NoActionClassVisitor inspectPath(Log log, Path path, boolean ignoreFailures) throws IOException {
		final byte[] data = Files.readAllBytes(path);
		final ClassReader reader = new ClassReader(data);
		final NoActionClassVisitor visitor = new NoActionClassVisitor(log, ignoreFailures);
		reader.accept(visitor, ClassReader.SKIP_FRAMES);

		return visitor;
	}
}
