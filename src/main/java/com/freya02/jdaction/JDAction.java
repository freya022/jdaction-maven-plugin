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

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JDAction {
	public static NoActionClassVisitor inspectPath(Path path) throws IOException {
		final byte[] data = Files.readAllBytes(path);
		final ClassReader reader = new ClassReader(data);
		final NoActionClassVisitor visitor = new NoActionClassVisitor();
		reader.accept(visitor, ClassReader.SKIP_FRAMES);

		return visitor;
	}
}
