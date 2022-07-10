package com.freya02.jdaction;

import com.freya02.jdaction.test.ExpectedIssue;
import com.freya02.jdaction.test.GenerateTests;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeneratedTest {
	private static final Log log = new SystemStreamLog();
	private static Map<Integer, ExpectedIssue> expectedIssuesMap;

	@BeforeAll
	public static void setup() {
		expectedIssuesMap = GenerateTests.generate();
	}

	@Test
	public void test() throws Exception {
		final byte[] data = Files.readAllBytes(Paths.get("target", "test-classes", "NoActionTest.class"));
		final ClassReader reader = new ClassReader(data);
		final NoActionClassVisitor visitor = new NoActionClassVisitor(log, false);
		reader.accept(visitor, ClassReader.SKIP_FRAMES);

		final Map<Integer, String> issues = visitor.getIssues();

		List<ExpectedIssue> missedIssues = expectedIssuesMap
				.entrySet()
				.stream()
				.filter(entry -> !issues.containsKey(entry.getKey()))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());

		Assertions.assertTrue(missedIssues.isEmpty(), () -> {
			final String expected = missedIssues.stream()
					.map(ExpectedIssue::getTargetMethod)
					.collect(Collectors.joining("\n"));
			return "Some methods could not be detected by JDAction:\n" + expected;
		});
	}
}
