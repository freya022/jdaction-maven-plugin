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

package com.freya02.jdaction;

import com.freya02.jdaction.test.ExpectedIssue;
import com.freya02.jdaction.test.GenerateTests;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneratedTest {
	private static Map<Integer, ExpectedIssue> expectedIssuesMap;

	@BeforeAll
	public static void setup() {
		expectedIssuesMap = GenerateTests.generate();
	}

	@Test
	public void test() throws Exception {
		final NoActionClassVisitor visitor = JDAction.inspectPath(Paths.get("target", "test-classes", "NoActionTest.class"));

		final Set<Integer> issueLines = visitor.getIssueLines();

		List<ExpectedIssue> missedIssues = expectedIssuesMap
				.entrySet()
				.stream()
				.filter(entry -> !issueLines.contains(entry.getKey()))
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
