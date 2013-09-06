/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import java.io.IOException;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class RefactoringTests18 extends RefactoringTest {

	private static final Class clazz= RefactoringTests18.class;

	private static final String REFACTORING_PATH= "Refactoring18/ExtractInterface/";

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

	private void validateExtractInterface(String className, String newInterfaceName) throws JavaModelException, Exception, IOException {
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

}
