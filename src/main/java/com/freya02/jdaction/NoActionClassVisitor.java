package com.freya02.jdaction;

import org.apache.maven.plugin.logging.Log;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Map;

// Straight up from https://github.com/JDA-Applications/jdaction/blob/master/src/main/java/com/sedmelluq/discord/jdaction/NoActionClassVisitor.java
//  With a few modifications to what the logger shows and how issues are reported
public class NoActionClassVisitor extends ClassVisitor {
	public static final boolean isIJ = System.getProperties().getProperty("idea.version") != null;

	private final Log logger;
	private final boolean ignoreFailures;
	private String simpleSourceFile;

	private final Map<Integer, String> issues = new HashMap<>();

	NoActionClassVisitor(Log log, boolean ignoreFailures) {
		super(Opcodes.ASM9);
		this.logger = log;
		this.ignoreFailures = ignoreFailures;
	}

	@Override
	public void visitSource(String source, String debug) {
		this.simpleSourceFile = source;

		super.visitSource(source, debug);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new UnusedReturnMethodVisitor();
	}

	/** For testing purpose, this might be inaccurate as one line could have multiple instructions in java */
	Map<Integer, String> getIssues() {
		return issues;
	}

	public int getIssueCount() {
		return issues.size();
	}

	public String getSimpleSourceFile() {
		return simpleSourceFile;
	}

	private class UnusedReturnMethodVisitor extends MethodVisitor {
		private boolean checkForImmediatePop;
		private int lineNumber;

		public UnusedReturnMethodVisitor() {
			super(Opcodes.ASM9);
		}

		@Override
		public void visitLineNumber(int line, Label start) {
			lineNumber = line;
		}

		@Override
		public void visitInsn(int opcode) {
			if (checkForImmediatePop && (opcode == Opcodes.POP || opcode == Opcodes.POP2)) {
				final String simpleSourceFile = getSimpleSourceFile();

				final String sourceHyperlink = isIJ ? String.format(" (%s:%d)", simpleSourceFile, lineNumber) : "";
				final String realErrorMessage = String.format("Return value is unused. This action is not performed.%s", sourceHyperlink);
				String message = String.format("%s:%d %s", simpleSourceFile, lineNumber, realErrorMessage);

				if (isIJ) { //Small hack to add a hyperlink that IJ recognizes, to facilitate navigation
					message = message + " " + realErrorMessage;
				}

				if (ignoreFailures) {
					logger.warn(message);
				} else {
					logger.error(message);
				}

				issues.put(lineNumber, simpleSourceFile);
			}

			checkForImmediatePop = false;
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			checkForImmediatePop = NoActionTargetDetector.isRestActionDescriptor(desc);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitLdcInsn(Object cst) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitIincInsn(int var, int increment) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			checkForImmediatePop = false;
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			checkForImmediatePop = false;
		}
	}
}
