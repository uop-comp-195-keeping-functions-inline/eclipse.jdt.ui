/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.keys.CharacterKey;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.KeyStroke;
import org.eclipse.ui.keys.ModifierKey;
import org.eclipse.ui.keys.SWTKeySupport;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

public class MoveMembersWizard extends RefactoringWizard {

	public MoveMembersWizard(MoveStaticMembersRefactoring ref) {
		super(ref, RefactoringMessages.getString("MoveMembersWizard.page_title")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new MoveMembersInputPage());
	}
	
	private static class MoveMembersInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "MoveMembersInputPage"; //$NON-NLS-1$
		private static final int LABEL_FLAGS= JavaElementLabels.ALL_DEFAULT;

		private Combo fDestinationField;
		private static final int MRU_COUNT= 10;
		private static List fgMruDestinations= new ArrayList(MRU_COUNT);
		private ContentAssistant fContentAssistant;

		public MoveMembersInputPage() {
			super(PAGE_NAME, true);
		}
	
		public void setVisible(boolean visible){
			if (visible){
				String message= RefactoringMessages.getFormattedString("MoveMembersInputPage.descriptionKey", //$NON-NLS-1$
					new String[]{new Integer(getMoveRefactoring().getMembersToMove().length).toString(),
								 JavaModelUtil.getFullyQualifiedName(getMoveRefactoring().getDeclaringType())});
				setDescription(message);
			}	
			super.setVisible(visible);	
		}
	
		public void createControl(Composite parent) {		
			Composite composite= new Composite(parent, SWT.NONE);
			GridLayout gl= new GridLayout();
			gl.numColumns= 2;
			composite.setLayout(gl);
		
			addLabel(composite);
			addDestinationControls(composite);
		
			setControl(composite);
			Dialog.applyDialogFont(composite);
			WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MOVE_MEMBERS_WIZARD_PAGE);
		}

		private void addLabel(Composite parent) {
			Label label= new Label(parent, SWT.NONE);
			IMember[] members= getMoveRefactoring().getMembersToMove();
			if (members.length == 1) {
				label.setText(RefactoringMessages.getFormattedString(
						"MoveMembersInputPage.destination_single", //$NON-NLS-1$
						JavaElementLabels.getElementLabel(members[0], LABEL_FLAGS)));
			} else {
				label.setText(RefactoringMessages.getFormattedString(
						"MoveMembersInputPage.destination_multi", //$NON-NLS-1$
						String.valueOf(members.length)));
			}
			GridData gd= new GridData();
			gd.horizontalSpan= 2;
			label.setLayoutData(gd);
		}

		private void addDestinationControls(Composite composite) {
			fDestinationField= new Combo(composite, SWT.SINGLE | SWT.BORDER);
			fDestinationField.setFocus();
			fDestinationField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDestinationField.setItems((String[]) fgMruDestinations.toArray(new String[fgMruDestinations.size()]));
			fDestinationField.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					handleDestinationChanged();
				}
				private void handleDestinationChanged() {
					IStatus status= JavaConventions.validateJavaTypeName(fDestinationField.getText());
					if (status.getSeverity() == IStatus.ERROR){
						error(status.getMessage());
					} else {
						try {
							IType resolvedType= getMoveRefactoring().getDeclaringType().getJavaProject().findType(fDestinationField.getText());
							IStatus validationStatus= validateDestinationType(resolvedType, fDestinationField.getText());
							if (validationStatus.isOK()){
								setErrorMessage(null);
								setPageComplete(true);
							} else {
								error(validationStatus.getMessage());
							}
						} catch(JavaModelException ex) {
							JavaPlugin.log(ex); //no ui here
							error(RefactoringMessages.getString("MoveMembersInputPage.invalid_name")); //$NON-NLS-1$
						}
					}
				}
				private void error(String message){
					setErrorMessage(message);
					setPageComplete(false);
				}
			});
			if (fgMruDestinations.size() > 0) {
				fDestinationField.select(0);
			} else {
				setPageComplete(false);
			}
			fContentAssistant= createContentAssistant(fDestinationField);
			
			Button button= new Button(composite, SWT.PUSH);
			button.setText(RefactoringMessages.getString("MoveMembersInputPage.browse")); //$NON-NLS-1$
			button.setLayoutData(new GridData());
			SWTUtil.setButtonDimensionHint(button);
			button.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					openTypeSelectionDialog();
				}
			});
		}
			
		private ContentAssistant createContentAssistant(final Combo combo) {
			final ContentAssistant contentAssistant= new ContentAssistant();
						
			IType declaringType= getMoveRefactoring().getDeclaringType();
			IContentAssistProcessor processor= new TypeContentAssistProcessor(declaringType);
			contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
			
			ContentAssistPreference.configure(contentAssistant, JavaPlugin.getDefault().getJavaTextTools().getPreferenceStore());
			contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
				public IInformationControl createInformationControl(Shell parent) {
					return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true));
				}
			});

			combo.addKeyListener(getContentAssistKeyAdapter(contentAssistant));
			contentAssistant.install(new ComboContentAssistSubjectAdapter(combo));
			return contentAssistant;
		}

		private KeyAdapter getContentAssistKeyAdapter(final ContentAssistant contentAssistant) {
			return new KeyAdapter() {
				KeySequence[] fKeySequences;
				
				private KeySequence[] getKeySequences() {
					if (fKeySequences == null) {
						ICommandManager cm = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
						ICommand command= cm.getCommand(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
						if (command.isDefined()) {
							List list= command.getKeySequenceBindings();
							if (!list.isEmpty()) {
								fKeySequences= new KeySequence[list.size()];
								for (int i= 0; i < fKeySequences.length; i++) {
									fKeySequences[i]= ((IKeySequenceBinding) list.get(i)).getKeySequence();
								}
								return fKeySequences;
							}		
						}
						// default is Ctrl+Space
						fKeySequences= new KeySequence[] { 
								KeySequence.getInstance(KeyStroke.getInstance(ModifierKey.CTRL, CharacterKey.SPACE))
						};
					}
					return fKeySequences;
				}
				
				public void keyPressed(KeyEvent e) {
					int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
					KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
					KeySequence[] sequences= getKeySequences();
					
					for (int i= 0; i < sequences.length; i++) {
						// only works for single strokes (would need to hold KeyBindingState for multiple):
						if (sequences[i].equals(keySequence)) {
							e.doit= false;
							String errorMessage= contentAssistant.showPossibleCompletions();
							if (errorMessage != null)
								setErrorMessage(errorMessage);
							return;
						}
					}
				}};
		}

		public void dispose() {
			if (fContentAssistant != null) {
				fContentAssistant.uninstall();
				fContentAssistant= null;
				super.dispose();
			}
		}
		
		protected boolean performFinish() {
			initializeRefactoring();
			return super.performFinish();
		}
	
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		private void initializeRefactoring() {
			try {
				String destination= fDestinationField.getText();
				if (!fgMruDestinations.remove(destination) && fgMruDestinations.size() >= MRU_COUNT)
					fgMruDestinations.remove(fgMruDestinations.size() - 1);
				fgMruDestinations.add(0, destination);
				
				getMoveRefactoring().setDestinationTypeFullyQualifiedName(destination);
			} catch(JavaModelException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("MoveMembersInputPage.move_Member"), RefactoringMessages.getString("MoveMembersInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	
		private IJavaSearchScope createWorkspaceSourceScope(){
			try {
				return RefactoringScopeFactory.create(getMoveRefactoring().getDeclaringType().getJavaProject());
			} catch (JavaModelException e) {
				//fallback is the whole workspace scope
				String title= RefactoringMessages.getString("MoveMembersInputPage.move"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("MoveMembersInputPage.internal_error"); //$NON-NLS-1$
				ExceptionHandler.handle(e, getShell(), title, message);
				return SearchEngine.createJavaSearchScope(new IJavaElement[]{getMoveRefactoring().getDeclaringType().getJavaProject()}, true);
			}
		}
	
		private void openTypeSelectionDialog(){
			int elementKinds= IJavaSearchConstants.TYPE;
			final IJavaSearchScope scope= createWorkspaceSourceScope();
			TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), elementKinds, scope);
			dialog.setTitle(RefactoringMessages.getString("MoveMembersInputPage.choose_Type")); //$NON-NLS-1$
			dialog.setMessage(RefactoringMessages.getString("MoveMembersInputPage.dialogMessage")); //$NON-NLS-1$
			dialog.setUpperListLabel(RefactoringMessages.getString("MoveMembersInputPage.upperListLabel")); //$NON-NLS-1$
			dialog.setLowerListLabel(RefactoringMessages.getString("MoveMembersInputPage.lowerListLabel")); //$NON-NLS-1$
			dialog.setValidator(new ISelectionStatusValidator(){
				public IStatus validate(Object[] selection) {
					Assert.isTrue(selection.length <= 1);
					if (selection.length == 0)
						return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.Invalid_selection"), null); //$NON-NLS-1$
					Object element= selection[0];
					if (! (element instanceof TypeInfo))
						return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.Invalid_selection"), null); //$NON-NLS-1$
					try {
						TypeInfo info= (TypeInfo)element;
						return validateDestinationType(info.resolveType(scope), info.getTypeName());
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
						return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.internal_error"), null); //$NON-NLS-1$
					}
				}
			});
			dialog.setMatchEmptyString(false);
			dialog.setFilter(createInitialFilter());
			if (dialog.open() == Window.CANCEL)
				return;
			IType firstResult= (IType)dialog.getFirstResult();		
			fDestinationField.setText(JavaModelUtil.getFullyQualifiedName(firstResult));	
		}

		private String createInitialFilter() {
			if (! fDestinationField.getText().trim().equals("")) //$NON-NLS-1$
				return fDestinationField.getText();
			else
				return getMoveRefactoring().getDeclaringType().getElementName();
		}
	
		private static IStatus validateDestinationType(IType type, String typeName){
			if (type == null || ! type.exists())
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getFormattedString("MoveMembersInputPage.not_found", typeName), null); //$NON-NLS-1$
			if (type.isBinary())
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.no_binary"), null); //$NON-NLS-1$
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	
		private MoveStaticMembersRefactoring getMoveRefactoring(){
			return (MoveStaticMembersRefactoring)getRefactoring();
		}
	}
}
