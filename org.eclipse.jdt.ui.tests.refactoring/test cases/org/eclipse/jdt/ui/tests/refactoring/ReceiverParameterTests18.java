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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
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

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor.MemberActionInfo;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ReceiverParameterTests18 extends RefactoringTest {

	private static final Class clazz= ReceiverParameterTests18.class;

	private static final String REFACTORING_PATH= "ReceiverParameter/";

	private Hashtable fOldOptions;

	public ReceiverParameterTests18(String name) {
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

	private static ExtractSupertypeProcessor createExtractSuperclassRefactoringProcessor(IMember[] members) throws JavaModelException {
		IJavaProject project= null;
		if (members != null && members.length > 0)
			project= members[0].getJavaProject();
		if (RefactoringAvailabilityTester.isExtractSupertypeAvailable(members)) {
			final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(project);
			settings.createComments= false;
			ExtractSupertypeProcessor processor= new ExtractSupertypeProcessor(members, settings);
			new ProcessorBasedRefactoring(processor);
			return processor;
		}
		return null;
	}

	private IPackageFragment getPackage(String name) throws JavaModelException {
		if ("p".equals(name))
			return getPackageP();
		IPackageFragment pack= getRoot().getPackageFragment(name);
		if (pack.exists())
			return pack;
		return getRoot().createPackageFragment(name, false, new NullProgressMonitor());
	}

	private static String getTopLevelTypeName(String typeQualifiedTyperName) {
		int dotIndex= typeQualifiedTyperName.indexOf('.');
		if (dotIndex == -1)
			return typeQualifiedTyperName;
		return typeQualifiedTyperName.substring(0, dotIndex);
	}

	private void prepareForInputCheck(PushDownRefactoringProcessor processor, IMethod[] selectedMethods, IField[] selectedFields, String[] namesOfMethodsToPullUp,
			String[][] signaturesOfMethodsToPullUp, String[] namesOfFieldsToPullUp, String[] namesOfMethodsToDeclareAbstract, String[][] signaturesOfMethodsToDeclareAbstract) {
		IMethod[] methodsToPushDown= findMethods(selectedMethods, namesOfMethodsToPullUp, signaturesOfMethodsToPullUp);
		IField[] fieldsToPushDown= findFields(selectedFields, namesOfFieldsToPullUp);
		List membersToPushDown= Arrays.asList(merge(methodsToPushDown, fieldsToPushDown));
		List methodsToDeclareAbstract= Arrays.asList(findMethods(selectedMethods, namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract));

		MemberActionInfo[] infos= processor.getMemberActionInfos();
		for (int i= 0; i < infos.length; i++) {
			if (membersToPushDown.contains(infos[i].getMember())) {
				infos[i].setAction(MemberActionInfo.PUSH_DOWN_ACTION);
				assertTrue(!methodsToDeclareAbstract.contains(infos[i].getMember()));
			}
			if (methodsToDeclareAbstract.contains(infos[i].getMember())) {
				infos[i].setAction(MemberActionInfo.PUSH_ABSTRACT_ACTION);
				assertTrue(!membersToPushDown.contains(infos[i].getMember()));
			}
		}
	}

	/**
	 * Tests "Pull Up" method refactoring involving receiver parameter.
	 * 
	 * @throws Exception any exception thrown from this test case
	 */
	public void testPullUp() throws Exception {
		String[] methodNames= new String[] { "foo1", "foo2" };
		String[][] signatures= new String[][] { new String[]{"QList;"}, new String[] { "QString;" } };
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		IMethod[] methods= getMethods(cuB.getType("B"), methodNames, signatures);

		PullUpRefactoringProcessor processor= createPullUpRefactoringProcessor(methods);
		Refactoring ref= processor.getRefactoring();

		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());

		IType[] possibleClasses= processor.getCandidateTypes(new RefactoringStatus(), new NullProgressMonitor());
		assertTrue("No possible class found!", possibleClasses.length > 0);
		processor.setDestinationType(processor.getCandidateTypes(new RefactoringStatus(), new NullProgressMonitor())[possibleClasses.length - 1 - 0]);

		if (deleteAllInSourceType)
			processor.setDeletedMethods(methods);
		if (deleteAllMatchingMethods) {
			List l= Arrays.asList(JavaElementUtil.getElementsOfType(processor.getMatchingElements(new NullProgressMonitor(), false), IJavaElement.METHOD));
			processor.setDeletedMethods((IMethod[])l.toArray(new IMethod[l.size()]));
		}

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass", !checkInputResult.hasError());
		performChange(ref, false);

		String expected= getFileContents(getOutputTestFileName("A"));
		String actual= cuA.getSource();
		assertEqualLines(expected, actual);
		expected= getFileContents(getOutputTestFileName("B"));
		actual= cuB.getSource();
		assertEqualLines(expected, actual);
	}

	/**
	 * Tests "Push Down" method refactoring involving receiver parameter.
	 * 
	 * @throws Exception any exception thrown from this test case
	 */
	public void testPushDown() throws Exception {
		String[] namesOfMethodsToPushDown= { "foo1", "foo2" };
		String[][] signaturesOfMethodsToPushDown= { new String[0], new String[] { "QString;" } };
		String[] selectedFieldNames= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};


		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");

		IType type= getType(cuA, "A");
		IMethod[] selectedMethods= getMethods(type, namesOfMethodsToPushDown, signaturesOfMethodsToPushDown);
		IField[] selectedFields= getFields(type, selectedFieldNames);
		IMember[] selectedMembers= merge(selectedFields, selectedMethods);

		assertTrue(RefactoringAvailabilityTester.isPushDownAvailable(selectedMembers));
		PushDownRefactoringProcessor processor= new PushDownRefactoringProcessor(selectedMembers);
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());

		prepareForInputCheck(processor, selectedMethods, selectedFields, namesOfMethodsToPushDown, signaturesOfMethodsToPushDown, namesOfFieldsToPushDown, namesOfMethodsToDeclareAbstract,
				signaturesOfMethodsToDeclareAbstract);

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass but got " + checkInputResult.toString(), !checkInputResult.hasError());
		performChange(ref, false);

		String expected= getFileContents(getOutputTestFileName("A"));
		String actual= cuA.getSource();
		assertEqualLines("A.java", expected, actual);

	}

	/**
	 * Tests "Move" method refactoring involving receiver parameter.
	 * 
	 * @throws Exception any exception thrown from this test case
	 */
	public void testMove() throws Exception {
		String[] cuQNames= new String[] { "p1.A", "p2.B" };
		String selectionCuQName= "p1.A";
		boolean inlineDelegator= false;
		boolean removeDelegator= false;
		int selectionCuIndex= -1;
		for (int i= 0; i < cuQNames.length; i++)
			if (cuQNames[i] == null || selectionCuQName.equals(cuQNames[i]))
				selectionCuIndex= i;
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		ICompilationUnit cuA= createCUfromTestFile(getPackage("p1"), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackage("p1"), "B");

		int offset= cuA.getSource().indexOf("mA1(@NonNull A this, @NonNull B b)");
		IJavaElement[] codeSelect= cuA.codeSelect(offset, 3);
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
		processor.setMethodName("mA1Moved");

		preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass", !preconditionResult.hasError());

		performChange(ref, false);

		String outputTestFileName= getOutputTestFileName("A");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuA.getSource());


		outputTestFileName= getOutputTestFileName("B");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuB.getSource());

	}

	/**
	 * Tests "Move" method refactoring where the target is type annotated.
	 *
	 * @throws Exception any exception thrown from this test case
	 */
	public void testMove2() throws Exception {
		String[] cuQNames= new String[] { "p1.A", "p2.B" };
		String selectionCuQName= "p1.A";
		boolean inlineDelegator= false;
		boolean removeDelegator= false;
		int selectionCuIndex= -1;
		for (int i= 0; i < cuQNames.length; i++)
			if (cuQNames[i] == null || selectionCuQName.equals(cuQNames[i]))
				selectionCuIndex= i;
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		ICompilationUnit cuA= createCUfromTestFile(getPackage("p1"), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackage("p1"), "B");

		int offset= cuA.getSource().indexOf("mA1(@Nullable A this, @NonNull B b)");
		IJavaElement[] codeSelect= cuA.codeSelect(offset, 3);
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
		processor.setMethodName("mA1Moved");

		preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass", !preconditionResult.hasError());

		performChange(ref, false);

		String outputTestFileName= getOutputTestFileName("A");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuA.getSource());


		outputTestFileName= getOutputTestFileName("B");
		assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cuB.getSource());

	}

	/**
	 * Tests "Move Type to New File" refactoring involving receiver parameter.
	 * 
	 * @throws Exception any exception thrown from this test case
	 */
	public void testMoveType2File() throws Exception {
		String parentClassName= "A";
		String enclosingInstanceName= "Inner";
		String[] cuNames= new String[] { "A" };
		String[] packageNames= new String[] { "p" };
		String packageName= "p";
		IType parentClas= getType(createCUfromTestFile(getPackage(packageName), parentClassName), parentClassName);
		IType clas= parentClas.getType(enclosingInstanceName);

		assertTrue("should be enabled", RefactoringAvailabilityTester.isMoveInnerAvailable(clas));
		MoveInnerToTopRefactoring ref= ((RefactoringAvailabilityTester.isMoveInnerAvailable(clas)) ? new MoveInnerToTopRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas
				.getJavaProject())) : null);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("activation was supposed to be successful" + preconditionResult.toString(), preconditionResult.isOK());

		assertEquals("reference creation possible", true, ref.isCreatingInstanceFieldPossible());
		assertEquals("reference creation mandatory", false, ref.isCreatingInstanceFieldMandatory());
		if (ref.isCreatingInstanceFieldPossible() && !ref.isCreatingInstanceFieldMandatory())
			ref.setCreateInstanceField(false);
		ref.setEnclosingInstanceName(enclosingInstanceName);
		assertTrue("name should be ok ", ref.checkEnclosingInstanceName(enclosingInstanceName).isOK());
		ref.setMarkInstanceFieldAsFinal(false);
		ICompilationUnit[] cus= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			if (cuNames[i].equals(clas.getCompilationUnit().findPrimaryType().getElementName()))
				cus[i]= clas.getCompilationUnit();
			else
				cus[i]= createCUfromTestFile(getPackage(packageNames[i]), cuNames[i]);
		}

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass", !checkInputResult.hasError());
		performChange(ref, false);

		for (int i= 0; i < cus.length; i++) {
			String actual= cus[i].getSource();
			String expected= getFileContents(getOutputTestFileName(cuNames[i]));
			assertEqualLines(cus[i].getElementName(), expected, actual);
		}
		ICompilationUnit newCu= clas.getPackageFragment().getCompilationUnit(enclosingInstanceName + ".java");
		String expected= getFileContents(getOutputTestFileName(enclosingInstanceName));
		String actual= newCu.getSource();
		assertEqualLines("new Cu:", expected, actual);

	}

	/**
	 * Tests "Extract Interface" refactoring involving receiver parameter.
	 * 
	 * @throws Exception any exception thrown from this test case
	 */
	public void testExtractInterface() throws Exception {
		String className= "A";
		String newInterfaceName= "I";

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
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		assertEqualLines("incorrect changes in " + className,
				getFileContents(getOutputTestFileName(className)),
				cu.getSource());

		ICompilationUnit interfaceCu= pack.getCompilationUnit(newInterfaceName + ".java");
		assertEqualLines("incorrect interface created",
				getFileContents(getOutputTestFileName(newInterfaceName)),
				interfaceCu.getSource());
	}

	/**
	 * Tests "Extract Superclass" refactoring involving receiver parameter.
	 * 
	 * @throws Exception any exception thrown from this test case
	 */
	public void testExtractSuperclass() throws Exception {
		String[] methodNames= new String[] { "foo1", "foo2" };
		String[][] signatures= new String[][] { new String[0], new String[] { "QString;" } };
		boolean replaceOccurences= true;


		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType type= getType(cu, "A");
		IMethod[] methods= getMethods(type, methodNames, signatures);

		ExtractSupertypeProcessor processor= createExtractSuperclassRefactoringProcessor(methods);
		Refactoring refactoring= processor.getRefactoring();
		processor.setMembersToMove(methods);

		assertTrue("activation", refactoring.checkInitialConditions(new NullProgressMonitor()).isOK());

		processor.setTypesToExtract(new IType[] { type });
		processor.setTypeName("B");
		processor.setCreateMethodStubs(true);
		processor.setInstanceOf(false);
		processor.setReplace(replaceOccurences);
		processor.setDeletedMethods(methods);

		RefactoringStatus status= refactoring.checkFinalConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass", !status.hasError());
		performChange(refactoring, false);

		String expected= getFileContents(getOutputTestFileName("A"));
		String actual= cu.getSource();
		assertEqualLines(expected, actual);

		expected= getFileContents(getOutputTestFileName("B"));
		ICompilationUnit unit= getPackageP().getCompilationUnit("B.java");
		if (!unit.exists())
			assertTrue("extracted compilation unit does not exist", false);
		actual= unit.getBuffer().getContents();
		assertEqualLines(expected, actual);


	}

}
