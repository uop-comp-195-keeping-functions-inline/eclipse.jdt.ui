/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Action group that adds the open and show actions to a context menu and
 * the action bar's navigate menu. This action group reuses the <code>
 * OpenEditorActionGroup</code>, <code>OpenViewActionGroup</code>
 * and <code>ShowActionGroup</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class NavigateActionGroup extends ActionGroup {

	private OpenEditorActionGroup fOpenEditorActionGroup;
	private OpenViewActionGroup fOpenViewActionGroup;
	private ShowActionGroup fShowActionGroup;
	
	/**
	 * Creates a new <code>NavigateActionGroup</code>. The group requires
	 * that the selection provided by the part's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public NavigateActionGroup(IViewPart  part) {
		fOpenEditorActionGroup= new OpenEditorActionGroup(part);
		fOpenViewActionGroup= new OpenViewActionGroup(part);
		fShowActionGroup= new ShowActionGroup(part);
	}

	/**
	 * Returns the open action managed by this action group. 
	 * 
	 * @return the open action. Returns <code>null</code> if the group
	 * 	doesn't provide any open action
	 */
	public IAction getOpenAction() {
		return fOpenEditorActionGroup.getOpenAction();
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void dispose() {
		super.dispose();
		fOpenEditorActionGroup.dispose();
		fOpenViewActionGroup.dispose();
		fShowActionGroup.dispose();
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		fOpenEditorActionGroup.fillActionBars(actionBars);
		fOpenViewActionGroup.fillActionBars(actionBars);
		fShowActionGroup.fillActionBars(actionBars);
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		
		fOpenEditorActionGroup.fillContextMenu(menu);
		fOpenViewActionGroup.fillContextMenu(menu);
		fShowActionGroup.fillContextMenu(menu);
		
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void setContext(ActionContext context) {
		super.setContext(context);
		fOpenEditorActionGroup.setContext(context);
		fOpenViewActionGroup.setContext(context);
		fShowActionGroup.setContext(context);
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void updateActionBars() {
		super.updateActionBars();
		fOpenEditorActionGroup.updateActionBars();
		fOpenViewActionGroup.updateActionBars();
		fShowActionGroup.updateActionBars();
	}
}
