/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

/**
 * Java element method return type hyperlink detector.
 * 
 * @since 3.7
 */
public class JavaElementHyperlinkReturnTypeDetector extends JavaElementHyperlinkDetector {


	@Override
	protected void addHyperlinks(List<IHyperlink> hyperlinksCollector, IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		try {
			if (element.getElementType() == IJavaElement.METHOD && !JavaModelUtil.isPrimitive(((IMethod)element).getReturnType()) && SelectionConverter.canOperateOn(editor)) {
				hyperlinksCollector.add(new JavaElementReturnTypeHyperlink(wordRegion, openAction, (IMethod)element, qualify));
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}
}
