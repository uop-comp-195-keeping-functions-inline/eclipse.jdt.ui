/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

public class SubtypeConstraint extends SimpleTypeConstraint implements ITypeConstraint {

	public SubtypeConstraint(ConstraintVariable left, ConstraintVariable right) {
		super(left, right);
	}

	/* (non-Javadoc)
	 * @see experiments.AbstractTypeConstraint#isSatisfied()
	 */
	public boolean isSatisfied() {
		return getLeft().isSubtypeOf(getRight());
	}

	public String toString(){
		return getLeft().toString() + " <= " + getRight().toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.experiments.TypeConstraint#toResolvedString()
	 */
	public String toResolvedString() {
		return getLeft().toResolvedString() + " <= " + getRight().toResolvedString();
	}
}
