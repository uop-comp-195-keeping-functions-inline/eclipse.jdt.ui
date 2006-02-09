/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.NewCUCompletionUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.template.contentassist.SurroundWithTemplateProposal;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

/**
  */
public class QuickFixTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite();
//		suite.addTest(SerialVersionQuickFixTest.allTests());
		suite.addTest(UtilitiesTest.allTests());
		suite.addTest(UnresolvedTypesQuickFixTest.allTests());
		suite.addTest(UnresolvedVariablesQuickFixTest.allTests());
		suite.addTest(UnresolvedMethodsQuickFixTest.allTests());
		suite.addTest(ReturnTypeQuickFixTest.allTests());
		suite.addTest(LocalCorrectionsQuickFixTest.allTests());
		suite.addTest(TypeMismatchQuickFixTests.allTests());
		suite.addTest(ReorgQuickFixTest.allTests());
		suite.addTest(ModifierCorrectionsQuickFixTest.allTests());
		suite.addTest(AssistQuickFixTest.allTests());
		suite.addTest(ChangeNonStaticToStaticTest.suite());
		suite.addTest(MarkerResolutionTest.allTests());
		suite.addTest(JavadocQuickFixTest.allTests());
		suite.addTest(ConvertForLoopQuickFixTest.allTests());
		suite.addTest(ConvertIterableLoopQuickFixTest.allTests());
		suite.addTest(AdvancedQuickAssistTest.allTests());
		suite.addTest(CleanUpTest.allTests());
		suite.addTest(QuickFixEnablementTest.allTests());
		
		return new ProjectTestSetup(suite);
	}

	
	public QuickFixTest(String name) {
		super(name);
	}
	
	public static void assertCorrectLabels(List proposals) {
		for (int i= 0; i < proposals.size(); i++) {
			ICompletionProposal proposal= (ICompletionProposal) proposals.get(i);
			String name= proposal.getDisplayString();
			if (name == null || name.length() == 0 || name.charAt(0) == '!' || name.indexOf("{0}") != -1 || name.indexOf("{1}") != -1) {
				assertTrue("wrong proposal label: " + name, false);
			}
			if (proposal.getImage() == null) {
				assertTrue("wrong proposal image", false);
			}			
		}
	}
	
	public static void assertCorrectContext(IInvocationContext context, ProblemLocation problem) {
		if (problem.getProblemId() != 0) {
			if (!JavaCorrectionProcessor.hasCorrections(context.getCompilationUnit(), problem.getProblemId())) {
				assertTrue("Problem type not marked with light bulb: " + problem, false);
			}
		}
	}	
	
	
	public static void assertNumberOf(String name, int nProblems, int nProblemsExpected) {
		assertTrue("Wrong number of " + name + ", is: " + nProblems + ", expected: " + nProblemsExpected, nProblems == nProblemsExpected);
	}
	
	
	public static void assertEqualString(String actual, String expected) {	
		StringAsserts.assertEqualString(actual, expected);
	}
	
	public static void assertEqualStringIgnoreDelim(String actual, String expected) throws IOException {
		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}	
	
	public static void assertEqualStringsIgnoreOrder(String[] actuals, String[] expecteds) {
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expecteds);			
	}
	
	
	public static void assertExpectedExistInProposals(List actualProposals, String[] expecteds) throws CoreException, BadLocationException {
		StringAsserts.assertExpectedExistInProposals(getPreviewContents(actualProposals), expecteds);
	}
	
	public static TypeDeclaration findTypeDeclaration(CompilationUnit astRoot, String simpleTypeName) {
		List types= astRoot.types();
		for (int i= 0; i < types.size(); i++) {
			TypeDeclaration elem= (TypeDeclaration) types.get(i);
			if (simpleTypeName.equals(elem.getName().getIdentifier())) {
				return elem;
			}
		}
		return null;
	}
	
	public static MethodDeclaration findMethodDeclaration(TypeDeclaration typeDecl, String methodName) {
		MethodDeclaration[] methods= typeDecl.getMethods();
		for (int i= 0; i < methods.length; i++) {
			if (methodName.equals(methods[i].getName().getIdentifier())) {
				return methods[i];
			}
		}
		return null;
	}
	
	public static VariableDeclarationFragment findFieldDeclaration(TypeDeclaration typeDecl, String fieldName) {
		FieldDeclaration[] fields= typeDecl.getFields();
		for (int i= 0; i < fields.length; i++) {
			List list= fields[i].fragments();
			for (int k= 0; k < list.size(); k++) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment) list.get(k);
				if (fieldName.equals(fragment.getName().getIdentifier())) {
					return fragment;
				}				
			}
		}
		return null;
	}	
		
	public static AssistContext getCorrectionContext(ICompilationUnit cu, int offset, int length) {
		AssistContext context= new AssistContext(cu, offset, length);
		return context;
	}


	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot) throws CoreException {
		return collectCorrections(cu, astRoot, 1, null);
	}
	
	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems) throws CoreException {
		return collectCorrections(cu, astRoot, nProblems, null);
	}


	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems, AssistContext context) throws CoreException {
		IProblem[] problems= astRoot.getProblems();
		if (problems.length != nProblems) {
			StringBuffer buf= new StringBuffer("Wrong number of problems, is: ");
			buf.append(problems.length).append(", expected: ").append(nProblems).append('\n');
			for (int i= 0; i < problems.length; i++) {
				buf.append(problems[i]);
				buf.append('[').append(problems[i].getSourceStart()).append(" ,").append(problems[i].getSourceEnd()).append(']');
				buf.append('\n');
			}
			assertTrue(buf.toString(), false);

		}
		return collectCorrections(cu, problems[0], context);
	}
	
	protected static final ArrayList collectCorrections2(ICompilationUnit cu, int nProblems) throws CoreException {
		
		final ArrayList problemsList= new ArrayList();
		IProblemRequestor requestor= new IProblemRequestor() {
			public void acceptProblem(IProblem problem) {
				problemsList.add(problem);
			}
			public void beginReporting() {
				problemsList.clear();
			}
			public void endReporting() {}
			public boolean isActive() {	return true;}
		};
		
		ICompilationUnit wc= cu.getWorkingCopy(new WorkingCopyOwner() {}, requestor, null);
		try {
			wc.reconcile(ICompilationUnit.NO_AST, true, true, wc.getOwner(), null);
		} finally {
			wc.discardWorkingCopy();
		}
		
		IProblem[] problems= (IProblem[]) problemsList.toArray(new IProblem[problemsList.size()]);
		if (problems.length != nProblems) {
			StringBuffer buf= new StringBuffer("Wrong number of problems, is: ");
			buf.append(problems.length).append(", expected: ").append(nProblems).append('\n');
			for (int i= 0; i < problems.length; i++) {
				buf.append(problems[i]);
				buf.append('[').append(problems[i].getSourceStart()).append(" ,").append(problems[i].getSourceEnd()).append(']');
				buf.append('\n');
			}
			assertTrue(buf.toString(), false);

		}
		return collectCorrections(cu, problems[0], null);
	}
	
	protected static final ArrayList collectCorrections(ICompilationUnit cu, IProblem curr, IInvocationContext context) throws CoreException {
		int offset= curr.getSourceStart();
		int length= curr.getSourceEnd() + 1 - offset;
		if (context == null) {
			context= new AssistContext(cu, offset, length);
		}
		
		ProblemLocation problem= new ProblemLocation(offset, length, curr.getID(), curr.getArguments(), curr.isError());
		ArrayList proposals= collectCorrections(context, problem);
		if (!proposals.isEmpty()) {
			assertCorrectContext(context, problem);
		}
		
		return proposals;
	}
	
	protected static ArrayList collectCorrections(IInvocationContext context, IProblemLocation problem) throws CoreException {
		ArrayList proposals= new ArrayList();
		IStatus status= JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { problem }, proposals);
		assertStatusOk(status);
		return proposals;
	}
	
	public static void assertStatusOk(IStatus status) throws CoreException {
		if (!status.isOK()) {
			if (status.getException() == null) {  // find a status with an exception
				IStatus[] children= status.getChildren();
				for (int i= 0; i < children.length; i++) {
					IStatus child= children[i];
					if (child.getException() != null) {
						throw new CoreException(child);
					}
				}
			}
		}
	}
	
	
	protected static final ArrayList collectAssists(IInvocationContext context, Class[] filteredTypes) throws CoreException {
		ArrayList proposals= new ArrayList();
		IStatus status= JavaCorrectionProcessor.collectAssists(context, new IProblemLocation[0], proposals);
		assertStatusOk(status);
		
		if (!proposals.isEmpty()) {
			assertTrue("should be marked as 'has assist'", JavaCorrectionProcessor.hasAssists(context));
		}
		
		
		if (filteredTypes != null && filteredTypes.length > 0) {
			for (Iterator iter= proposals.iterator(); iter.hasNext(); ) {
				if (isFiltered(iter.next(), filteredTypes)) {
					iter.remove();
				}
			}
		}
		return proposals;
	}
	
	private static boolean isFiltered(Object curr, Class[] filteredTypes) {
		for (int k = 0; k < filteredTypes.length; k++) {
			if (filteredTypes[k].isInstance(curr)) {
				return true;
			}
		}
		return false;
	}
	
	protected static final ArrayList collectAssists(IInvocationContext context, boolean includeLinkedRename) throws CoreException {
		Class[] filteredTypes= includeLinkedRename ? null : new Class[] { LinkedNamesAssistProposal.class };
		return collectAssists(context, filteredTypes);
	}
	
	protected static CompilationUnit getASTRoot(ICompilationUnit cu) {
		return ASTResolving.createQuickFixAST(cu, null);
	}
	
	
	protected static String[] getPreviewContents(List proposals) throws CoreException, BadLocationException {
		String[] res= new String[proposals.size()];
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof CUCorrectionProposal) {
				res[i]= getPreviewContent((CUCorrectionProposal) curr);
			} else if (curr instanceof NewCUCompletionUsingWizardProposal) {
				res[i]= getWizardPreviewContent((NewCUCompletionUsingWizardProposal) curr);
			} else if (curr instanceof SurroundWithTemplateProposal) {
				res[i]= getTemplatePreviewContent((SurroundWithTemplateProposal)curr);
			}
		}
		return res;
	}
	
	private static String getTemplatePreviewContent(SurroundWithTemplateProposal proposal) {
		return proposal.getPreviewContent();
	}

	protected static String getPreviewContent(CUCorrectionProposal proposal) throws CoreException {
		return proposal.getPreviewContent();
	}
	
	protected static String getWizardPreviewContent(NewCUCompletionUsingWizardProposal newCUWizard) throws CoreException, BadLocationException {
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		IType createdType= newCUWizard.getCreatedType();
		assertTrue("Nothing created", createdType.exists());
		String preview= createdType.getCompilationUnit().getSource();
		
		IJavaElement parent= createdType.getParent();
		if (parent instanceof IType) {
			createdType.delete(true, null);
		} else {
			JavaProjectHelper.delete(parent);
		}
		StringBuffer res= new StringBuffer();
		IDocument doc= new Document(preview);
		int nLines= doc.getNumberOfLines();
		for (int i= 0; i < nLines; i++) {
			IRegion lineInformation= doc.getLineInformation(i);
			res.append(doc.get(lineInformation.getOffset(), lineInformation.getLength()));
			if (i != nLines - 1) {
				res.append('\n');
			}
		}
		return res.toString();
	}
	
	protected static void assertNumberOfProposals(List proposals, int expectedProposals) {
		if (proposals.size() != expectedProposals) {
			StringBuffer buf= new StringBuffer();
			buf.append("Wrong number of proposals, is: ").append(proposals.size()). append(", expected: ").append(expectedProposals).append('\n');
			for (int i= 0; i < proposals.size(); i++) {
				ICompletionProposal curr= (ICompletionProposal) proposals.get(i);
				buf.append(" - ").append(curr.getDisplayString()).append('\n');
				if (curr instanceof CUCorrectionProposal) {
					appendSource(((CUCorrectionProposal) curr), buf);
				}
			}
			assertTrue(buf.toString(), false);
		}
	}
	
	private static void appendSource(CUCorrectionProposal proposal, StringBuffer buf) {
		
	}
	
	protected static void assertNoErrors(IInvocationContext context) {
		IProblem[] problems= context.getASTRoot().getProblems();
		for (int i= 0; i < problems.length; i++) {
			if (problems[i].isError()) {
				assertTrue("source has error: " + problems[i].getMessage(), false);
			}
		}
	}
	
	public static String getPreviewsInBufAppend(ICompilationUnit cu) throws CoreException, BadLocationException {
		CompilationUnit astRoot= getASTRoot(cu);
		List proposals= collectCorrections(cu, astRoot);
		if (proposals.isEmpty()) {
			return null;
		}
		return getPreviewsInBufAppend(cu, proposals);
	}
		
	
	protected static String getPreviewsInBufAppend(ICompilationUnit cu, List proposals) throws CoreException, BadLocationException {
		StringBuffer buf= new StringBuffer();
		String[] previewContents= getPreviewContents(proposals);
		
		buf.append("public void testX() throws Exception {\n");
		buf.append("IPackageFragment pack1= fSourceFolder.createPackageFragment(\"").append(cu.getParent().getElementName()).append("\", false, null);\n");
		buf.append("StringBuffer buf= new StringBuffer();\n");
		wrapInBufAppend(cu.getBuffer().getContents(), buf);
		buf.append("ICompilationUnit cu= pack1.createCompilationUnit(\"").append(cu.getElementName()).append("\", buf.toString(), false, null);\n\n");
		buf.append("CompilationUnit astRoot= getASTRoot(cu);\n");
		buf.append("ArrayList proposals= collectCorrections(cu, astRoot);\n\n");
		buf.append("assertCorrectLabels(proposals);\n");
		
		buf.append("assertNumberOfProposals(proposals, ").append(previewContents.length).append(");\n\n");
		buf.append("String[] expected= new String[").append(previewContents.length).append("];\n");
		
		for (int i= 0; i < previewContents.length; i++) {
			String curr= previewContents[i];
			if (curr == null) {
				continue;
			}
			
			buf.append("buf= new StringBuffer();\n");
			wrapInBufAppend(curr, buf);
			buf.append("expected[" + i + "]= buf.toString();\n\n");
		}
		
		buf.append("assertExpectedExistInProposals(proposals, expected);\n");
		buf.append("}\n");
		return buf.toString();
	}


	private static void wrapInBufAppend(String curr, StringBuffer buf) {
		buf.append("buf.append(\"");
		
		int last= curr.length() - 1;
		for (int k= 0; k <= last ; k++) {
			char ch= curr.charAt(k);
			if (ch == '\n') {
				buf.append("\\n\");\n");
				if (k < last) {
					buf.append("buf.append(\"");
				}
			} else if (ch == '\r') {
				// ignore
			} else if (ch == '\t') {
				buf.append("    "); // 4 spaces
			} else if (ch == '"' || ch == '\\') {
				buf.append('\\').append(ch);
			} else {
				buf.append(ch);
			}
		}
		if (buf.length() > 0 && buf.charAt(buf.length() - 1) != '\n') {
			buf.append("\\n\");\n");
		}
	}
	
	
}
