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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.viewsupport.MemberFilter;
import org.eclipse.jdt.internal.ui.viewsupport.MemberFilterAction;

/**
 * Action Group that contributes filter buttons for a view parts showing 
 * methods and fields. Contributed filters are: hide fields, hide static
 * members and hide non-public members.
 * <p>
 * The action group installs a filter on a structured viewer. The filter is connected 
 * to the actions installed in the view part's toolbar menu and is updated when the 
 * state of the buttons changes.
 *  
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class MemberFilterActionGroup extends ActionGroup {

	public static final int FILTER_NONPUBLIC= MemberFilter.FILTER_NONPUBLIC;
	public static final int FILTER_STATIC= MemberFilter.FILTER_STATIC;
	public static final int FILTER_FIELDS= MemberFilter.FILTER_FIELDS;
	
	private static final String TAG_HIDEFIELDS= "hidefields"; //$NON-NLS-1$
	private static final String TAG_HIDESTATIC= "hidestatic"; //$NON-NLS-1$
	private static final String TAG_HIDENONPUBLIC= "hidenonpublic"; //$NON-NLS-1$
	
	private MemberFilterAction[] fFilterActions;
	private MemberFilter fFilter;
	
	private StructuredViewer fViewer;
	private String fViewerId;
	
	
	/**
	 * Creates a new <code>MemberFilterActionGroup</code>.
	 * 
	 * @param viewer the viewer to be filtered
	 * @param viewerId a unique id of the viewer. Used as a key to to store 
	 * the last used filter settings in the preference store
	 */
	public MemberFilterActionGroup(StructuredViewer viewer, String viewerId) {
		fViewer= viewer;
		fViewerId= viewerId;
		
		// get initial values
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean doHideFields= store.getBoolean(getPreferenceKey(FILTER_FIELDS));
		boolean doHideStatic= store.getBoolean(getPreferenceKey(FILTER_STATIC));
		boolean doHidePublic= store.getBoolean(getPreferenceKey(FILTER_NONPUBLIC));

		fFilter= new MemberFilter();
		if (doHideFields)
			fFilter.addFilter(FILTER_FIELDS);
		if (doHideStatic)
			fFilter.addFilter(FILTER_STATIC);			
		if (doHidePublic)
			fFilter.addFilter(FILTER_NONPUBLIC);		
	
		// fields
		String title= ActionMessages.getString("MemberFilterActionGroup.hide_fields.label"); //$NON-NLS-1$
		String helpContext= IJavaHelpContextIds.FILTER_FIELDS_ACTION;
		MemberFilterAction hideFields= new MemberFilterAction(this, title, FILTER_FIELDS, helpContext, doHideFields);
		hideFields.setDescription(ActionMessages.getString("MemberFilterActionGroup.hide_fields.description")); //$NON-NLS-1$
		hideFields.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.hide_fields.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideFields, "fields_co.gif"); //$NON-NLS-1$
		
		// static
		title= ActionMessages.getString("MemberFilterActionGroup.hide_static.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_STATIC_ACTION;
		MemberFilterAction hideStatic= new MemberFilterAction(this, title, FILTER_STATIC, helpContext, doHideStatic);
		hideStatic.setDescription(ActionMessages.getString("MemberFilterActionGroup.hide_static.description")); //$NON-NLS-1$
		hideStatic.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.hide_static.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideStatic, "static_co.gif"); //$NON-NLS-1$
		
		// non-public
		title= ActionMessages.getString("MemberFilterActionGroup.hide_nonpublic.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_PUBLIC_ACTION;
		MemberFilterAction hideNonPublic= new MemberFilterAction(this, title, FILTER_NONPUBLIC, helpContext, doHidePublic);
		hideNonPublic.setDescription(ActionMessages.getString("MemberFilterActionGroup.hide_nonpublic.description")); //$NON-NLS-1$
		hideNonPublic.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.hide_nonpublic.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideNonPublic, "public_co.gif"); //$NON-NLS-1$
	
		// order corresponds to order in toolbar
		fFilterActions= new MemberFilterAction[] { hideFields, hideStatic, hideNonPublic };
		
		fViewer.addFilter(fFilter);
	}
	
	private String getPreferenceKey(int filterProperty) {
		return "MemberFilterActionGroup." + fViewerId + '.' + String.valueOf(filterProperty); //$NON-NLS-1$
	}
	
	/**
	 * Sets the member filters.
	 * 
	 * @param filterProperty the filter to be manipulated. Valid values are <code>FILTER_FIELDS</code>, 
	 * <code>FILTER_PUBLIC</code>, and <code>FILTER_PRIVATE</code> as defined by this action 
	 * group
	 * @param set if <code>true</code> the given filter is installed. If <code>false</code> the
	 * given filter is removed
	 * .
	 */	
	public void setMemberFilter(int filterProperty, boolean set) {
		setMemberFilters(new int[] {filterProperty}, new boolean[] {set}, true);
	}

	private void setMemberFilters(int[] propertyKeys, boolean[] propertyValues, boolean refresh) {
		if (propertyKeys.length == 0)
			return;
		Assert.isTrue(propertyKeys.length == propertyValues.length);
		
		for (int i= 0; i < propertyKeys.length; i++) {
			int filterProperty= propertyKeys[i];
			boolean set= propertyValues[i];
			if (set) {
				fFilter.addFilter(filterProperty);
			} else {
				fFilter.removeFilter(filterProperty);
			}
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			
			for (int j= 0; j < fFilterActions.length; j++) {
				int currProperty= fFilterActions[j].getFilterProperty();
				if (currProperty == filterProperty) {
					fFilterActions[j].setChecked(set);
				}
				store.setValue(getPreferenceKey(currProperty), hasMemberFilter(currProperty));
			}
		}
		if (refresh) {
			fViewer.getControl().setRedraw(false);
			fViewer.refresh();
			fViewer.getControl().setRedraw(true);
		}
	}

	/**
	 * Returns <code>true</code> if the given filter is installed.
	 * 
	 * @param filterProperty the filter to be tested. Valid values are <code>FILTER_FIELDS</code>, 
	 * <code>FILTER_PUBLIC</code>, and <code>FILTER_PRIVATE</code> as defined by this action 
	 * group
	 */	
	public boolean hasMemberFilter(int filterProperty) {
		return fFilter.hasFilter(filterProperty);
	}
	
	/**
	 * Saves the state of the filter actions in a memento.
	 * 
	 * @param memento the memento to which the state is saved
	 */
	public void saveState(IMemento memento) {
		memento.putString(TAG_HIDEFIELDS, String.valueOf(hasMemberFilter(FILTER_FIELDS)));
		memento.putString(TAG_HIDESTATIC, String.valueOf(hasMemberFilter(FILTER_STATIC)));
		memento.putString(TAG_HIDENONPUBLIC, String.valueOf(hasMemberFilter(FILTER_NONPUBLIC)));
	}
	
	/**
	 * Restores the state of the filter actions from a memento.
	 * <p>
	 * Note: This method does not refresh the viewer.
	 * </p>
	 * @param memento the memento from which the state is restored
	 */	
	public void restoreState(IMemento memento) {
		setMemberFilters(
			new int[] {FILTER_FIELDS, FILTER_STATIC, FILTER_NONPUBLIC},
			new boolean[] {
				Boolean.valueOf(memento.getString(TAG_HIDEFIELDS)).booleanValue(),
				Boolean.valueOf(memento.getString(TAG_HIDESTATIC)).booleanValue(),
				Boolean.valueOf(memento.getString(TAG_HIDENONPUBLIC)).booleanValue()
			}, false);
	}
	
	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		contributeToToolBar(actionBars.getToolBarManager());
	};
	
	/**
	 * Adds the filter actions to the given tool bar
	 * 
	 * @param tbm the tool bar to which the actions are added
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(fFilterActions[0]); // fields
		tbm.add(fFilterActions[1]); // static
		tbm.add(fFilterActions[2]); // public
	}
	
	/* (non-Javadoc)
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		fViewer.removeFilter(fFilter);
		super.dispose();
	}

}
