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
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class QuickFixTest18 extends QuickFixTest {

	private static final Class THIS= QuickFixTest18.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public QuickFixTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= Java18ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public void testUnimplementedMethods() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    int getCount(Object[] o) throws IOException;\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int getCount(Object[] o) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	public void testUnimplementedMethods2() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class MyString implements CharSequence{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyString.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class MyString implements CharSequence{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class MyString implements CharSequence{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public char charAt(int arg0) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public int length() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public CharSequence subSequence(int arg0, int arg1) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	// bug 420116 : test for annotated varargs and return type
	public void testUnimplementedMethods3() throws Exception {
		File bundleFile= FileLocator.getBundleFile(Platform.getBundle("org.eclipse.jdt.annotation"));
		String JAR_PATH;
		if (bundleFile.isDirectory())
			JAR_PATH= bundleFile.getPath() + "/bin";
		else
			JAR_PATH= bundleFile.getPath();
		JavaProjectHelper.addLibrary(fJProject1, new Path(JAR_PATH));

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);


		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public interface List {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("List.java", buf.toString(), false, null);

		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.List;\n\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public interface Inter {\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException;\n");
		buf.append("    public int bug(@NonNull String... s) throws IOException;\n");
		buf.append("    public @NonNull String bar(@NonNull String s, @Nullable List<String> l1, test2.@NonNull List l2);\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.List;\n\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public int bug(@NonNull String... s) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public @NonNull\n");
		buf.append("    String bar(@NonNull String s, @Nullable List<String> l1,\n");
		buf.append("            test2.@NonNull List l2) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	// bug 420116 : test for annotated varargs and return type
	public void testUnimplementedMethods4() throws Exception {
		Bundle[] bundle= Platform.getBundles("org.eclipse.jdt.annotation", "[2.0.0,3.0.0)");
		assertTrue("Required version of annotation jar could not be found!", bundle != null && bundle.length > 0);
		File bundleFile= FileLocator.getBundleFile(bundle[0]);
		String JAR_PATH;
		if (bundleFile.isDirectory())
			JAR_PATH= bundleFile.getPath() + "/bin";
		else
			JAR_PATH= bundleFile.getPath();
		JavaProjectHelper.addLibrary(fJProject1, new Path(JAR_PATH));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public interface List {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("List.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface ReadOnly {\n");
		buf.append("\n");
		buf.append("}\n");
		pack1.createCompilationUnit("ReadOnly.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n\n");
		buf.append("import test2.List;\n\n");
		buf.append("\n");
		buf.append("interface Inter {\n");
		buf.append("\n");
		buf.append("    int foo1(@Nullable Object[] o1, Object @Nullable [] o2);\n");
		buf.append("\n");
		buf.append("    int foo2(Object @Nullable... o1);\n");
		buf.append("\n");
		buf.append("    String @ReadOnly [] foo3(Object @Nullable [] o1, @Nullable Object... o2);\n");
		buf.append("\n");
		buf.append("    @ReadOnly String[] foo4(@Nullable String s1, @NonNull String... s2);\n");
		buf.append("\n");
		buf.append("    @ReadOnly Integer foo5(@Nullable List l1, java.util.@NonNull List<String> l2);\n");
		buf.append("\n");
		buf.append("    int foo11(@ReadOnly Object[] o1, Object @ReadOnly [] o2);\n");
		buf.append("\n");
		buf.append("    int foo22(Object @ReadOnly... o1);\n");
		buf.append("\n");
		buf.append("    int foo33(Object @ReadOnly [] o1, @ReadOnly Object... o2);\n");
		buf.append("\n");
		buf.append("    void foo44(@ReadOnly String s1, @ReadOnly String... s2);\n");
		buf.append("\n");
		buf.append("    void foo55(@ReadOnly List l1, java.util.@ReadOnly List<String> l2);\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Inter.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cuE= pack1.createCompilationUnit("E.java", buf.toString(), false, null);


		CompilationUnit astRoot= getASTRoot(cuE);
		ArrayList proposals= collectCorrections(cuE, astRoot, 10);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n\n");
		buf.append("import test2.List;\n\n");
		buf.append("class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo1(@Nullable Object[] o1, Object @Nullable [] o2) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo2(Object @Nullable... o1) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public String @ReadOnly [] foo3(Object @Nullable [] o1,\n");
		buf.append("            @Nullable Object... o2) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public @ReadOnly\n");
		buf.append("    String[] foo4(@Nullable String s1, @NonNull String... s2) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public @ReadOnly\n");
		buf.append("    Integer foo5(@Nullable List l1, java.util.@NonNull List<String> l2) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo11(@ReadOnly Object[] o1, Object @ReadOnly [] o2) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo22(Object @ReadOnly... o1) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo33(Object @ReadOnly [] o1, @ReadOnly Object... o2) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo44(@ReadOnly String s1, @ReadOnly String... s2) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo55(@ReadOnly List l1, java.util.@ReadOnly List<String> l2) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testInvalidInterfaceMethodModifier1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    private static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Inter.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	public void testInvalidInterfaceMethodModifier2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    protected default int defaultMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Inter.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	public void testAbstractInterfaceMethodWithBody1() throws Exception {

		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public char m1(int arg0);\n\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public static char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public default char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	public void testAbstractInterfaceMethodWithBody2() throws Exception {

		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public abstract char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public abstract char m1(int arg0);\n\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public static char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public default char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	public void testCreateMethodQuickFix1() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= c.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("\n");
		buf.append("    public abstract int[] values();\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= c.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix2() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= Snippet.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("\n");
		buf.append("    public static int[] values() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= Snippet.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix3() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        default void foo() {\n");
		buf.append("            int[] a = values1();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal)proposals.get(0);
		CUCorrectionProposal proposal2= (CUCorrectionProposal)proposals.get(1);

		StringBuffer buf1= new StringBuffer();
		buf1.append("package test1;\n");
		buf1.append("public interface NestedInterfaceInInterface {\n");
		buf1.append("    interface Interface {\n");
		buf1.append("        default void foo() {\n");
		buf1.append("            int[] a = values1();\n");
		buf1.append("        }\n\n");
		buf1.append("        static int[] values1() {\n");
		buf1.append("            return null;\n");
		buf1.append("        }\n");
		buf1.append("    }\n");
		buf1.append("}\n");

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        default void foo() {\n");
		buf.append("            int[] a = values1();\n");
		buf.append("        }\n");
		buf.append("    }\n\n");
		buf.append("    static int[] values1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal1), getPreviewContent(proposal2) }, new String[] { buf1.toString(), buf.toString() });
	}

	public void testCreateMethodQuickFix4() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Arrays.sort(this.values2());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Arrays.sort(this.values2());\n");
		buf.append("        }\n\n");
		buf.append("        public int[] values2();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix5() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Object o = Interface.getGlobal();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Object o = Interface.getGlobal();\n");
		buf.append("        }\n\n");
		buf.append("        public static Object getGlobal() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix6() throws Exception {
		StringBuffer buf1= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf1.append("package test1;\n");
		buf1.append("public class NestedInterfaceInClass {\n");
		buf1.append("    public static final int total= 10;\n");
		buf1.append("    interface Interface {\n");
		buf1.append("        public default void foo() {\n");
		buf1.append("            int[] a = values1();\n");
		buf1.append("        }\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInClass.java", buf1.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal)proposals.get(0);
		CUCorrectionProposal proposal2= (CUCorrectionProposal)proposals.get(1);

		buf1= new StringBuffer();
		buf1.append("package test1;\n");
		buf1.append("public class NestedInterfaceInClass {\n");
		buf1.append("    public static final int total= 10;\n");
		buf1.append("    interface Interface {\n");
		buf1.append("        public default void foo() {\n");
		buf1.append("            int[] a = values1();\n");
		buf1.append("        }\n");
		buf1.append("\n");
		buf1.append("        public static int[] values1() {\n");
		buf1.append("            return null;\n");
		buf1.append("        }\n");
		buf1.append("    }\n");
		buf1.append("}\n");

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class NestedInterfaceInClass {\n");
		buf.append("    public static final int total= 10;\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            int[] a = values1();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public static int[] values1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal1), getPreviewContent(proposal2) }, new String[] { buf1.toString(), buf.toString() });
	}

	public void testCreateMethodQuickFix7() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class NestedInterfaceInClass {\n");
		buf.append("    int total= 10;\n");
		buf.append("    interface Interface {\n");
		buf.append("            int[] a = NestedInterfaceInClass.values1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInClass.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class NestedInterfaceInClass {\n");
		buf.append("    int total= 10;\n");
		buf.append("    interface Interface {\n");
		buf.append("            int[] a = NestedInterfaceInClass.values1();\n");
		buf.append("    }\n");
		buf.append("    static int[] values1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType1() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    int fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("        return 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    int fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("        return 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType2() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType3() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		// Inferred parameter type
		buf.append("        I i= (x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("        i.foo(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("        i.foo(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType4() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    I i2= (int x) -> {\n");
		buf.append("        x++;\n");
		buf.append("        System.out.println(x);\n");
		buf.append("    };\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    I i2= (int x) -> {\n");
		buf.append("        x++;\n");
		buf.append("        System.out.println(x);\n");
		buf.append("        return 0;\n");
		buf.append("    };\n");
		buf.append("    \n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testChangeModifierToStatic1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList proposals= collectCorrections(cu, astRoot, 4, 0);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    static int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    static int bar1() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    static void bar2() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 3);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    static int fun1() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}
}
