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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IActionBars;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.internal.ui.actions.MultiActionGroup;

/**
 * Adds view menus to switch between flat and hierarchical layout.
 * 
 * @since 2.1 */
class LayoutActionGroup extends MultiActionGroup {

	LayoutActionGroup(PackageExplorerPart packageExplorer) {
		super(createActions(packageExplorer), getSelectedState(packageExplorer));
	}

	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		contributeToViewMenu(actionBars.getMenuManager());
	}
	
	private void contributeToViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new Separator());

		// Create layout sub menu
		IMenuManager layoutSubMenu= new MenuManager("Layout"); //$NON-NLS-1$
		String layoutGroupName= PackagesMessages.getString("LayoutActionGroup.label"); //$NON-NLS-1$
		GroupMarker marker= new GroupMarker(layoutGroupName);
		viewMenu.add(marker);
		viewMenu.appendToGroup(layoutGroupName,layoutSubMenu);
		addActions(layoutSubMenu);
	}

	static int getSelectedState(PackageExplorerPart packageExplorer) {
		if (packageExplorer.isHierarchicalLayout())
			return 1;
		else
			return 0;
	}
	
	static IAction[] createActions(PackageExplorerPart packageExplorer) {
		IAction flatLayoutAction= new LayoutAction(packageExplorer, PackageExplorerPart.FLAT_LAYOUT);
		flatLayoutAction.setText(PackagesMessages.getString("LayoutActionGroup.flatLayoutAction.label")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(flatLayoutAction, "flatLayout.gif"); //$NON-NLS-1$
		IAction hierarchicalLayout= new LayoutAction(packageExplorer, PackageExplorerPart.HIERARCHICAL_LAYOUT);
		hierarchicalLayout.setText(PackagesMessages.getString("LayoutActionGroup.hierarchicalLayoutAction.label"));	  //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hierarchicalLayout, "hierarchicalLayout.gif"); //$NON-NLS-1$
		
		return new IAction[]{flatLayoutAction, hierarchicalLayout};
	}
}

class LayoutAction extends Action implements IAction {

	private int fLayout;
	private PackageExplorerPart fPackageExplorer;

	public LayoutAction(PackageExplorerPart packageExplorer, int state) {
		fLayout= state;
		fPackageExplorer= packageExplorer;
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (fPackageExplorer.getLayout() != fLayout)
			fPackageExplorer.setUpViewer(fLayout);
	}
}
