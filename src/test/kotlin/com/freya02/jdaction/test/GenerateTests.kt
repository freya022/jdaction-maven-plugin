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

package com.freya02.jdaction.test

import io.github.classgraph.*
import net.dv8tion.jda.api.requests.RestAction
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import javax.annotation.CheckReturnValue

private val logger = LoggerFactory.getLogger(GenerateTests::class.java)

private val template = """
public class NoActionTest {
    @SuppressWarnings({"ConstantConditions", "rawtypes", "RedundantCast", "unchecked"})
    public void test() {
[[code]]
    }
}
""".trimIndent()

object GenerateTests {
    private val testSource: MutableList<String> = arrayListOf()

    private var noActionLine = template.lines().indexOfFirst { s -> s.contains("[[code]]") } + 1
    private val noActionLines: MutableMap<Int, ExpectedIssue> = hashMapOf()

    @JvmStatic
    fun generate(): MutableMap<Int, ExpectedIssue> {
        ClassGraph()
            .enableMethodInfo()
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages("net.dv8tion.jda.api")
            .scan().use { scan ->
                scan.allClasses.forEach { processClass(it) }
            }

        val finalCode = template.replace(
            "[[code]]",
            testSource.joinToString("\n")
                .prependIndent("        "), false
        )
        val path = Paths.get("src", "test", "java", "NoActionTest.java")
        Files.write(path, finalCode.lines(), CREATE, TRUNCATE_EXISTING)

        val waitFor = ProcessBuilder()
            .apply {
                command("mvn.cmd", "compiler:testCompile")
                inheritIO()
            }
            .start()
            .waitFor()

        if (waitFor != 0) {
            throw IllegalStateException("'mvn compile' exited with code $waitFor")
        }

        return noActionLines
    }

    private fun processClass(classInfo: ClassInfo) {
        classInfo
            .declaredMethodInfo
            .filter { it.typeDescriptor.resultType is ClassRefTypeSignature }
            .forEach { methodInfo ->
                val resultType = methodInfo.typeDescriptor.resultType as ClassRefTypeSignature
                val returnClassInfo = resultType.classInfo
                    ?: return@forEach  //Ignore every non-JDA return type

                if (!returnClassInfo.name.endsWith("Action")) return@forEach
                if (!returnClassInfo.implementsInterface(RestAction::class.java)) return@forEach

                addTestLine(classInfo, methodInfo)

                //At this point, the return type is a rest action
                if (!methodInfo.hasAnnotation(CheckReturnValue::class.java)) {
                    if (isSuperMethodChecked(classInfo, methodInfo)) return@forEach

                    logger.debug("Method ${methodInfo.toSimpleSignature()} does not have CheckReturnValue but returns a RestAction")
                }
            }
    }

    private fun addTestLine(classInfo: ClassInfo, methodInfo: MethodInfo) {
        val className = classInfo.name.replace('$', '.')
        val methodName = methodInfo.name
        val nullParameters = methodInfo.parameterInfo.joinToString(", ") {
            getCastedDefaultValue(it.typeDescriptor)
        }

        testSource += "(($className) null).$methodName($nullParameters);"

        noActionLines[noActionLine++] = ExpectedIssue(methodInfo.toSimpleSignature())
    }

    private fun getCastedDefaultValue(typeSignature: TypeSignature) =
        when (typeSignature) {
            is BaseTypeSignature -> when (typeSignature.type.kotlin) {
                Boolean::class -> "false"
                Short::class, Byte::class, Int::class, Long::class, Double::class, Float::class, Char::class -> "($typeSignature) 0"
                else -> throw IllegalArgumentException(typeSignature.toString())
            }
            else -> "(${typeSignature.toString().replace('$', '.')}) null"
        }

    private fun isSuperMethodChecked(classInfo: ClassInfo, methodInfo: MethodInfo): Boolean {
        return (classInfo.superclasses + classInfo.interfaces)
            .flatMap { it.methodInfo }
            .any superClassCheck@{ methodInfo2 ->
                val sameName = methodInfo.name == methodInfo2.name
                val sameParameters =
                    methodInfo.parameterInfo.map { it.typeDescriptor } == methodInfo2.parameterInfo.map { it.typeDescriptor }
                if (sameName && sameParameters && methodInfo2.hasAnnotation(CheckReturnValue::class.java)) {
                    return@superClassCheck true
                }

                return@superClassCheck false
            }
    }

    private fun MethodInfo.toSimpleSignature(): String {
        val className = classInfo.name
        val methodName = this.name
        val parameters = this.parameterInfo.joinToString(", ") { parameterInfo ->
            parameterInfo.typeSignatureOrTypeDescriptor.toStringWithSimpleNames()
        }

        return "$className#$methodName($parameters)"
    }
}