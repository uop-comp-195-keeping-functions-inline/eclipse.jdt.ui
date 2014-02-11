/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java Community Process (JCP) and
 * is made available for testing and evaluation purposes only.
 * The code is not compatible with any specification of the JCP.
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class RefactoringTests18 extends RefactoringTest {

	private static final Class clazz= RefactoringTests18.class;

	private String REFACTORING_PATH= "Refactoring18/";

	private Hashtable fOldOptions;

	public RefactoringTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18Setup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID,
				"${package_declaration}" +
						System.getProperty("line.separator", "\n") +
						"${" + CodeTemplateContextType.TYPE_COMMENT + "}" +
						System.getProperty("line.separator", "\n") +
						"${type_declaration}", null);

		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/** typecomment template*/", null);

		fOldOptions= JavaCore.getOptions();

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, DefaultCodeFormatterConstants.TRUE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);

		JavaCore.setOptions(options);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		JavaCore.setOptions(fOldOptions);
		fOldOptions= null;
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private static String getTopLevelTypeName(String typeQualifiedTyperName) {
		int dotIndex= typeQualifiedTyperName.indexOf('.');
		if (dotIndex == -1)
			return typeQualifiedTyperName;
		return typeQualifiedTyperName.substring(0, dotIndex);
	}
	



	/**
	 * Tests "Extract Interface" refactoring involving receiver parameter.
	 * 
	 * @throws Exception any exception thrown from this test case
	 */
	public void testExtractInterfaceFromInterface() throws Exception {
		String className= "A";
		String newInterfaceName= "B";

		validateExtractInterface(className, newInterfaceName);
	}

	public void testExtractInterfaceFromClass() throws Exception {
		String className= "A";
		String newInterfaceName= "B";

		validateExtractInterface(className, newInterfaceName);
	}

	public void testExtractInterfaceFromAbstractClass() throws Exception {
		String className= "A";
		String newInterfaceName= "B";

		validateExtractInterface(className, newInterfaceName);
	}

	// Test for bug 426963
	public void testPullUpMethodToInterface() throws Exception {
		File bundleFile= FileLocator.getBundleFile(Platform.getBundle("org.eclipse.jdt.annotation"));
		String JAR_PATH;
		if (bundleFile.isDirectory())
			JAR_PATH= bundleFile.getPath() + "/bin";
		else
			JAR_PATH= bundleFile.getPath();
		JavaProjectHelper.addLibrary(getPackageP().getJavaProject(), new Path(JAR_PATH));

		REFACTORING_PATH+= "PullUp/";
		String[] methodNames= new String[] { "getArea" };
		String[][] signatures= new String[][] { new String[] { "QInteger;" } };
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		IMethod[] methods= getMethods(cuA.getType("A"), methodNames, signatures);

		PullUpRefactoringProcessor processor= createPullUpRefactoringProcessor(methods);
		Refactoring ref= processor.getRefactoring();

		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "Inter1");
		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());

		IType[] possibleClasses= processor.getCandidateTypes(new RefactoringStatus(), new NullProgressMonitor());
		assertTrue("No possible class found!", possibleClasses.length > 0);
		processor.setDestinationType(processor.getCandidateTypes(new RefactoringStatus(), new NullProgressMonitor())[possibleClasses.length - 1 - 0]);
		processor.setAbstractMethods(methods);
		processor.setMembersToMove(new IMethod[0]);

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass", !checkInputResult.hasError());
		performChange(ref, false);

		String expected= getFileContents(getOutputTestFileName("A"));
		String actual= cuA.getSource();
		assertEqualLines(expected, actual);
		expected= getFileContents(getOutputTestFileName("Inter1"));
		actual= cuB.getSource();
		assertEqualLines(expected, actual);

	}

	private void validateExtractInterface(String className, String newInterfaceName) throws JavaModelException, Exception, IOException {
		REFACTORING_PATH+= "ExtractInterface/";
		IType clas= getType(createCUfromTestFile(getPackageP(), getTopLevelTypeName(className)), className);
		ICompilationUnit cu= clas.getCompilationUnit();
		IPackageFragment pack= (IPackageFragment)cu.getParent();

		ExtractInterfaceProcessor processor= new ExtractInterfaceProcessor(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas.getJavaProject()));
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		processor.setTypeName(newInterfaceName);
		assertEquals("interface name should be accepted", RefactoringStatus.OK, processor.checkTypeName(newInterfaceName).getSeverity());

		processor.setExtractedMembers(processor.getExtractableMembers());
		processor.setReplace(true);
		processor.setAnnotations(false);
		RefactoringStatus performRefactoring= performRefactoring(ref);
		assertEquals("was supposed to pass", null, performRefactoring);
		assertEqualLines("incorrect changes in " + className,
				getFileContents(getOutputTestFileName(className)),
				cu.getSource());

		ICompilationUnit interfaceCu= pack.getCompilationUnit(newInterfaceName + ".java");
		assertEqualLines("incorrect interface created",
				getFileContents(getOutputTestFileName(newInterfaceName)),
				interfaceCu.getSource());
	}

	private static PullUpRefactoringProcessor createPullUpRefactoringProcessor(IMember[] methods) throws JavaModelException {
		IJavaProject project= null;
		if (methods != null && methods.length > 0)
			project= methods[0].getJavaProject();
		if (RefactoringAvailabilityTester.isPullUpAvailable(methods)) {
			PullUpRefactoringProcessor processor= new PullUpRefactoringProcessor(methods, JavaPreferencesSettings.getCodeGenerationSettings(project));
			new ProcessorBasedRefactoring(processor);
			return processor;
		}
		return null;
	}

	// test for bug 410056
	public void testMove1() throws Exception {
		REFACTORING_PATH+= "Move/";
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		boolean inlineDelegator= true;
		boolean removeDelegator= true;
		int selectionCuIndex= -1;
		for (int i= 0; i < cuQNames.length; i++)
			if (cuQNames[i] == null || selectionCuQName.equals(cuQNames[i]))
				selectionCuIndex= i;
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		int offset= cuA.getSource().indexOf("getDefaultName(B b)");
		IJavaElement[] codeSelect= cuA.codeSelect(offset, "getDefaultName".length());
		assertTrue(codeSelect.length > 0);
		assertTrue(codeSelect[0] instanceof IMethod);
		IMethod method= (IMethod)codeSelect[0];
		MoveInstanceMethodProcessor processor= new MoveInstanceMethodProcessor(method, JavaPreferencesSettings.getCodeGenerationSettings(cuA.getJavaProject()));
		Refactoring ref= new MoveRefactoring(processor);

		assertNotNull("refactoring should be created", ref);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", preconditionResult.isOK());

		IVariableBinding target= null;
		IVariableBinding[] targets= processor.getPossibleTargets();
		for (int i= 0; i < targets.length; i++) {
			IVariableBinding candidate= targets[i];
			if (candidate.getName().equals("b")) {
				target= candidate;
				break;
			}
		}
		assertNotNull("Expected new target not available.", target);
		processor.setTarget(target);

		processor.setInlineDelegator(inlineDelegator);
		processor.setRemoveDelegator(removeDelegator);
		processor.setDeprecateDelegates(false);
		assertTrue("Move refactoring is not available", RefactoringAvailabilityTester.isMoveMethodAvailable(method));
		RefactoringAvailabilityTester.isMoveMethodAvailable(method);

		preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass", !preconditionResult.hasError());

		performChange(ref, false);

		String outputTestFileName= getOutputTestFileName("A");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuA.getSource());


		outputTestFileName= getOutputTestFileName("B");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuB.getSource());

	}

	// test for bug 410056
	public void testMove2() throws Exception {
		REFACTORING_PATH+= "Move/";
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		boolean inlineDelegator= true;
		boolean removeDelegator= true;
		int selectionCuIndex= -1;
		for (int i= 0; i < cuQNames.length; i++)
			if (cuQNames[i] == null || selectionCuQName.equals(cuQNames[i]))
				selectionCuIndex= i;
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		int offset= cuA.getSource().indexOf("getDefaultName(B b)");
		IJavaElement[] codeSelect= cuA.codeSelect(offset, "getDefaultName".length());
		assertTrue(codeSelect.length > 0);
		assertTrue(codeSelect[0] instanceof IMethod);
		IMethod method= (IMethod)codeSelect[0];
		MoveInstanceMethodProcessor processor= new MoveInstanceMethodProcessor(method, JavaPreferencesSettings.getCodeGenerationSettings(cuA.getJavaProject()));
		Refactoring ref= new MoveRefactoring(processor);

		assertNotNull("refactoring should be created", ref);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", preconditionResult.isOK());

		IVariableBinding target= null;
		IVariableBinding[] targets= processor.getPossibleTargets();
		for (int i= 0; i < targets.length; i++) {
			IVariableBinding candidate= targets[i];
			if (candidate.getName().equals("b")) {
				target= candidate;
				break;
			}
		}
		assertNotNull("Expected new target not available.", target);
		assertTrue("Move refactoring is not available", RefactoringAvailabilityTester.isMoveMethodAvailable(method));
		processor.setTarget(target);

		processor.setInlineDelegator(inlineDelegator);
		processor.setRemoveDelegator(removeDelegator);
		processor.setDeprecateDelegates(false);

		preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass", !preconditionResult.hasError());

		performChange(ref, false);

		String outputTestFileName= getOutputTestFileName("A");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuA.getSource());


		outputTestFileName= getOutputTestFileName("B");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuB.getSource());


	}
}
