/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

public class IntroduceIndirectionTests18 extends IntroduceIndirectionTests{
	private static final Class clazz= IntroduceIndirectionTests18.class;

	public IntroduceIndirectionTests18(String name) {
		super(name);
	}

	public static Test setUpTest(Test test) {
		return new Java18Setup(test);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

// ---

	public void test18_35() throws Exception {
		helperPass(new String[] { "p.Foo" }, "d", "p.Foo", 5, 17, 5, 18);
	}

	public void test18_36() throws Exception {
		helperErr(new String[] { "p.Foo" }, "s", "p.Foo", 5, 16, 5, 17);
	}

	public void test18_37() throws Exception {
		helperPass(new String[] { "p.Foo" }, "a", "p.Foo", 4, 9, 4, 10);
	}

	public void test18_38() throws Exception {
		helperPass(new String[] { "p.C", "p.Foo" }, "d", "p.Foo", 4, 9, 4, 10);
	}

	public void test18_39() throws Exception {
		helperPass(new String[] { "p.C", "p.Foo" }, "s", "p.Foo", 4, 16, 4, 17);
	}

	public void test18_40() throws Exception {
		helperPass(new String[] { "p.Foo", "p.C" }, "d", "p.C", 5, 17, 5, 18);
	}
}
