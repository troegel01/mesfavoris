package mesfavoris.texteditor.internal;

import static mesfavoris.bookmarktype.BookmarkPropertiesProviderUtil.getFirstElement;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROPERTY_NAME;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_FILE_PATH;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_PROJECT_NAME;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_WORKSPACE_PATH;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

import mesfavoris.MesFavoris;
import mesfavoris.bookmarktype.AbstractBookmarkPropertiesProvider;
import mesfavoris.placeholders.IPathPlaceholderResolver;

public class WorkspaceFileBookmarkPropertiesProvider extends AbstractBookmarkPropertiesProvider {
	private final IPathPlaceholderResolver pathPlaceholderResolver;

	public WorkspaceFileBookmarkPropertiesProvider() {
		this(MesFavoris.getPathPlaceholderResolver());
	}

	public WorkspaceFileBookmarkPropertiesProvider(IPathPlaceholderResolver pathPlaceholders) {
		this.pathPlaceholderResolver = pathPlaceholders;
	}
	
	@Override
	public void addBookmarkProperties(Map<String, String> bookmarkProperties, IWorkbenchPart part,
			ISelection selection, IProgressMonitor monitor) {
		Object selected = getFirstElement(selection);
		IResource resource = Adapters.adapt(selected, IResource.class);
		if (!(resource instanceof IFile)) {
			return;
		}
		IFile file = (IFile)resource;
		
		putIfAbsent(bookmarkProperties, PROP_WORKSPACE_PATH, file.getFullPath().toPortableString());
		putIfAbsent(bookmarkProperties, PROP_PROJECT_NAME, file.getProject().getName());
		File localFile = file.getLocation().toFile();
		IPath filePath = Path.fromOSString(localFile.toString());
		addFilePath(bookmarkProperties, filePath);
		putIfAbsent(bookmarkProperties, PROPERTY_NAME, () ->  filePath.lastSegment());
	}

	private void addFilePath(Map<String, String> properties, IPath filePath) {
		putIfAbsent(properties, PROP_FILE_PATH, () -> pathPlaceholderResolver.collapse(filePath));
	}	
	
}
