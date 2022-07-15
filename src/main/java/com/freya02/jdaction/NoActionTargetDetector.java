/*
 * Copyright 2022 sedmelluq (me@sedmelluq.com), freya022, and the jdaction-maven-plugin contributors
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

import java.util.regex.Pattern;

//Straight up from https://github.com/JDA-Applications/jdaction/blob/master/src/main/java/com/sedmelluq/discord/jdaction/NoActionTargetDetector.java
public class NoActionTargetDetector {
	public static final Pattern SUBCLASS_PATTERN = Pattern.compile(
			"^\\)Lnet/dv8tion/jda/[a-z0-9/]+/restaction/([a-z0-9/]+/)?[a-zA-Z0-9]+Action;$"
	);

	public static boolean isRestActionDescriptor(String descriptor) {
		if (descriptor.endsWith(")Lnet/dv8tion/jda/api/requests/RestAction;")) {
			return true;
		}

		String suffix = descriptor.substring(descriptor.lastIndexOf(')'));
		return SUBCLASS_PATTERN.matcher(suffix).matches();
	}
}
