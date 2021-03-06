package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.IEditorStatusLine;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder.OccurrenceLocation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlinkDetector;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This action opens a popup Java editor on a Java element or file.
 * <p>
 * The action is applicable to selections containing elements of type <code>ICompilationUnit</code>,
 * <code>IMember</code> or <code>IFile</code>.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 3.18
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenPopupAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	private JavaEditor fOpenEditor;

	private boolean isOpen;

	/**
	 * Creates a new <code>OpenPopupAction</code>. The action requires that the selection provided
	 * by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public OpenPopupAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OpenPopupAction_label);
		setToolTipText(ActionMessages.OpenPopupAction_tooltip);
		setDescription(ActionMessages.OpenPopupAction_description);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public OpenPopupAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		isOpen= false;
		setText(ActionMessages.OpenPopupAction_declaration_label);
		setEnabled(EditorUtility.getEditorInputJavaElement(fEditor, false) != null);
	}

	@Override
	public void selectionChanged(ITextSelection selection) {
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		for (Iterator<?> iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof ISourceReference)
				continue;
			if (element instanceof IFile)
				continue;
			if (JavaModelUtil.isOpenableStorage(element))
				continue;
			return false;
		}
		return true;
	}

	@Override
	public void run(ITextSelection selection) {
		if (handleClose())
			return;
		ITypeRoot input= EditorUtility.getEditorInputJavaElement(fEditor, false);
		if (input == null) {
			setStatusLineMessage();
			return;
		}
		IRegion region= new Region(selection.getOffset(), selection.getLength());
		OccurrenceLocation location= JavaElementHyperlinkDetector.findBreakOrContinueTarget(input, region);
		if (location != null) {
			editorRevealAndHandle(location);
			return;
		}
		location= JavaElementHyperlinkDetector.findSwitchCaseTarget(input, region);
		if (location != null) {
			editorRevealAndHandle(location);
			return;
		}
		try {
			IJavaElement[] elements= SelectionConverter.codeResolveForked(fEditor, false);
			elements= selectOpenableElements(elements);
			if (elements == null || elements.length == 0) {
				if (!ActionUtil.isProcessable(fEditor))
					return;
				setStatusLineMessage();
				return;
			}

			IJavaElement element= elements[0];
			if (elements.length > 1) {
				if (needsUserSelection(elements, input)) {
					element= SelectionConverter.selectJavaElement(elements, getShell(), getDialogTitle(), ActionMessages.OpenPopupAction_select_element);
					if (element == null)
						return;
				}
			}

			run(new Object[] { element });
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.OpenPopupAction_error_message);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private void editorRevealAndHandle(OccurrenceLocation location) {
		IEditorPart oldEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		fEditor.selectAndReveal(location.getOffset(), location.getLength());
		IEditorPart newEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if ( !newEditor.equals(oldEditor) ) {
			EModelService service= newEditor.getSite().getService(EModelService.class);
			MPartSashContainerElement mPart= newEditor.getSite().getService(MPart.class);

			if (mPart.getCurSharedRef() != null)
				mPart= mPart.getCurSharedRef();

			service.detach(mPart, 100, 100, 300, 300);
		}
	}

	private boolean needsUserSelection(IJavaElement[] elements, ITypeRoot input) {
		if (elements[0] instanceof IPackageFragment) {
			IJavaProject javaProject= input.getJavaProject();
			if (JavaModelUtil.is9OrHigher(javaProject)) {
				try {
					if (javaProject.getModuleDescription() != null) {
						for (IJavaElement element : elements) {
							IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
							if (root.getModuleDescription() != null)
								return true;
						}
					}
				} catch (JavaModelException e) {
					// silent
				}
			}
			// below 9 or with no modules in the picture:
			// if there are multiple IPackageFragments that could be selected, use the first one on the build path.
			return false;
		}
		return true;
	}

	/**
	 * Sets the error message in the status line.
	 * 
	 * @since 3.7
	 */
	private void setStatusLineMessage() {
		IEditorStatusLine statusLine= fEditor.getAdapter(IEditorStatusLine.class);
		if (statusLine != null)
			statusLine.setMessage(true, ActionMessages.OpenPopupAction_error_messageBadSelection, null);
		getShell().getDisplay().beep();
		return;
	}

	/**
	 * Selects the openable elements out of the given ones.
	 *
	 * @param elements the elements to filter
	 * @return the openable elements
	 * @since 3.4
	 */
	private IJavaElement[] selectOpenableElements(IJavaElement[] elements) {
		List<IJavaElement> result= new ArrayList<>(elements.length);
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];
			switch (element.getElementType()) {
				case IJavaElement.PACKAGE_DECLARATION:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.JAVA_MODEL:
					break;
				default:
					result.add(element);
					break;
			}
		}
		return result.toArray(new IJavaElement[result.size()]);
	}

	@Override
	public void run(IStructuredSelection selection) {
		if (handleClose())
			return;
		if (!checkEnabled(selection))
			return;
		run(selection.toArray());
	}

	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 *
	 * @param elements the elements to process
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void run(Object[] elements) {
		if (elements == null)
			return;

		if (handleClose())
			return;

		MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, ActionMessages.OpenPopupAction_multistatus_message, null);

		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			try {
				Object javaElement= getElementToOpen(element);
				if (javaElement instanceof IPackageFragment) {
					if (fEditor == null) {
						try {
							PackageExplorerPart view= (PackageExplorerPart) JavaPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
							view.tryToReveal(element);
						} catch (PartInitException e) {
							JavaPlugin.log(e);
						}
					} else {
						setStatusLineMessage();
						return;
					}

				} else {
					javaElementHandle(javaElement);
				}
			} catch (PartInitException e) {
				String message= Messages.format(ActionMessages.OpenPopupAction_error_problem_opening_editor,
						new String[] { JavaElementLabels.getTextLabel(element, JavaElementLabels.ALL_DEFAULT), e.getStatus().getMessage() });
				status.add(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));
			} catch (CoreException e) {
				String message= Messages.format(ActionMessages.OpenPopupAction_error_problem_opening_editor,
						new String[] { JavaElementLabels.getTextLabel(element, JavaElementLabels.ALL_DEFAULT), e.getStatus().getMessage() });
				status.add(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));
				JavaPlugin.log(e);
			}
		}
		if (!status.isOK()) {
			IStatus[] children= status.getChildren();
			ErrorDialog.openError(getShell(), getDialogTitle(), ActionMessages.OpenPopupAction_error_message, children.length == 1 ? children[0] : status);
		}
	}

	private boolean handleClose() {
		if (isOpen && fOpenEditor != null) {
			if (fOpenEditor.isSaveOnCloseNeeded())
				fOpenEditor.close(true);
			else
				fOpenEditor.close(false);

			fOpenEditor= null;
			isOpen= false;

			return true;
		} else {
			return false;
		}
	}

	private void javaElementHandle(Object javaElement) throws PartInitException {
		boolean activateOnOpen= fEditor != null ? true : OpenStrategy.activateOnOpen();
		IEditorPart part= EditorUtility.openInEditor(javaElement, activateOnOpen);
		if (part != null && javaElement instanceof IJavaElement) {
			JavaUI.revealInEditor(part, (IJavaElement) javaElement);

			/* Check if javaElement's associated editor is new -> detach and position */
			if (!fEditor.toString().equals(part.toString())) {
				EModelService service= part.getSite().getService(EModelService.class);
				MPartSashContainerElement mPart= part.getSite().getService(MPart.class);

				if (mPart.getCurSharedRef() != null)
					mPart= mPart.getCurSharedRef();

				StyledText text= (StyledText) fEditor.getAdapter(Control.class);
				Point relPos= text.getLocationAtOffset(text.getCaretOffset());
				Point absPos= text.toDisplay(relPos);
				service.detach(mPart, absPos.x, absPos.y + text.getLineHeight(), 680, 350);

				fOpenEditor= (JavaEditor) part;
				isOpen= true;
			}
		}
	}

	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 *
	 * @param object the element to open
	 * @return the real element to open
	 * @throws JavaModelException if an error occurs while accessing the Java model
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Object getElementToOpen(Object object) throws JavaModelException {
		if (object instanceof IPackageFragment) {
			return getPackageFragmentObjectToOpen((IPackageFragment) object);
		}
		return object;
	}

	private Object getPackageFragmentObjectToOpen(IPackageFragment packageFragment) throws JavaModelException {
		ITypeRoot typeRoot= null;
		IPackageFragmentRoot root= (IPackageFragmentRoot) packageFragment.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (root.getKind() == IPackageFragmentRoot.K_BINARY)
			typeRoot= (packageFragment).getClassFile(JavaModelUtil.PACKAGE_INFO_CLASS);
		else
			typeRoot= (packageFragment).getCompilationUnit(JavaModelUtil.PACKAGE_INFO_JAVA);
		if (typeRoot.exists())
			return typeRoot;

		Object[] nonJavaResources= (packageFragment).getNonJavaResources();
		for (Object nonJavaResource : nonJavaResources) {
			if (nonJavaResource instanceof IFile) {
				IFile file= (IFile) nonJavaResource;
				if (file.exists() && JavaModelUtil.PACKAGE_HTML.equals(file.getName())) {
					return file;
				}
			}
		}
		return packageFragment;
	}

	private String getDialogTitle() {
		return ActionMessages.OpenPopupAction_error_title;
	}
}
