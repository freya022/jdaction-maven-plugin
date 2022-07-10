package com.freya02.jdaction;

import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Straight up from https://github.com/JDA-Applications/jdaction/blob/master/src/main/java/com/sedmelluq/discord/jdaction/NoActionClassVisitor.java
//  With a few modifications to what the logger shows and how issues are reported
public class NoActionClassVisitor extends ClassVisitor {
	public static final boolean isIJ = System.getProperties().getProperty("idea.version") != null;

	private String simpleSourceFile;

	private final Map<Integer, NoActionIssue> issues = new HashMap<>();
	private final HashSet<Integer> issueLines = new HashSet<>();

	NoActionClassVisitor() {
		super(Opcodes.ASM9);
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
	Set<Integer> getIssueLines() {
		return issueLines;
	}

	public Map<Integer, NoActionIssue> getIssues() {
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

				issueLines.add(lineNumber);
				issues.put(lineNumber, new NoActionIssue(simpleSourceFile, lineNumber));
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

	public static class NoActionIssue {
		private final String simpleSourceFile;
		private final int lineNumber;

		public NoActionIssue(String simpleSourceFile, int lineNumber) {
			this.simpleSourceFile = simpleSourceFile;
			this.lineNumber = lineNumber;
		}

		public String getSimpleSourceFile() {
			return simpleSourceFile;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public String getAsMessage() {
			final String sourceHyperlink = isIJ ? String.format(" (%s:%d)", simpleSourceFile, lineNumber) : "";
			final String realErrorMessage = String.format("Return value is unused. This action is not performed.%s", sourceHyperlink);
			String message = String.format("%s:%d %s", simpleSourceFile, lineNumber, realErrorMessage);

			if (isIJ) { //Small hack to add a hyperlink that IJ recognizes, to facilitate navigation
				message = message + " " + realErrorMessage;
			}

			return message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			NoActionIssue that = (NoActionIssue) o;

			if (lineNumber != that.lineNumber) return false;
			return simpleSourceFile.equals(that.simpleSourceFile);
		}

		@Override
		public int hashCode() {
			int result = simpleSourceFile.hashCode();
			result = 31 * result + lineNumber;
			return result;
		}

		@Override
		public String toString() {
			return String.format("%s:%d", simpleSourceFile, lineNumber);
		}
	}
}
