package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ResourceBundle;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;


public class ClassFileEditorActionContributor extends BasicTextEditorActionContributor {
	
	protected OpenOnSelectionAction fOpenOnSelection;
	protected OpenOnSelectionAction fOpenHierarchyOnSelection;
	protected TogglePresentationAction fTogglePresentationAction;
	protected RetargetTextEditorAction fShowJavaDoc;
	
	protected RetargetTextEditorAction fDisplay;
	protected RetargetTextEditorAction fInspect;
	
	/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
	protected ToggleTextHoverAction fToggleTextHover;
	
	
	public ClassFileEditorActionContributor() {
		super();
		
		ResourceBundle bundle= JavaEditorMessages.getResourceBundle();
		fOpenOnSelection= new OpenOnSelectionAction();
		fOpenHierarchyOnSelection= new OpenHierarchyOnSelectionAction();
		fTogglePresentationAction= new TogglePresentationAction();
		fShowJavaDoc= new RetargetTextEditorAction(bundle, "ShowJavaDoc."); //$NON-NLS-1$
		
		/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
		fToggleTextHover= new ToggleTextHoverAction();
		fDisplay= new RetargetTextEditorAction(bundle, "DisplayAction."); //$NON-NLS-1$	
		fInspect= new RetargetTextEditorAction(bundle, "InpsectAction."); //$NON-NLS-1$
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			
			editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenOnSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenHierarchyOnSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fShowJavaDoc);
			
			editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));	
			editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fInspect);		
			editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fDisplay);
		}
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator());
		tbm.add(fTogglePresentationAction);
		
		/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
		tbm.add(fToggleTextHover);
	}
	
	/**
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		
		super.setActiveEditor(part);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
		
		fOpenOnSelection.setContentEditor(textEditor);
		fOpenHierarchyOnSelection.setContentEditor(textEditor);
		
		fTogglePresentationAction.setEditor(textEditor);
		
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$

		/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
		fToggleTextHover.setEditor(textEditor);
		
		IAction updateAction= getAction(textEditor, "Display"); //$NON-NLS-1$
		if (updateAction instanceof IUpdate) {
			((IUpdate)updateAction).update();
		}
		fDisplay.setAction(updateAction); 
		updateAction= getAction(textEditor, "Inspect"); //$NON-NLS-1$
		if (updateAction instanceof IUpdate) {
			((IUpdate)updateAction).update();
		}
		fInspect.setAction(updateAction);
	}
}