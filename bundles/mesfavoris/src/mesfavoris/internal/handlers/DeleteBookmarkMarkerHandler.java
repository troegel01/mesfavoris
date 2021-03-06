package mesfavoris.internal.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;

import mesfavoris.MesFavoris;
import mesfavoris.handlers.AbstractBookmarkHandler;
import mesfavoris.internal.BookmarksPlugin;
import mesfavoris.internal.StatusHelper;
import mesfavoris.markers.IBookmarksMarkers;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;

public class DeleteBookmarkMarkerHandler extends AbstractBookmarkHandler {
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarksMarkers bookmarksMarkers;

	public DeleteBookmarkMarkerHandler() {
		this.bookmarkDatabase = MesFavoris.getBookmarkDatabase();
		this.bookmarksMarkers = BookmarksPlugin.getDefault().getBookmarksMarkers();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		if (selection.isEmpty()) {
			return null;
		}

		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		try {
			progressService.busyCursorWhile(monitor -> {
				Set<Bookmark> bookmarks = getSelectedBookmarksRecursively(bookmarkDatabase.getBookmarksTree(),
						selection, bookmark->!(bookmark instanceof BookmarkFolder));
				SubMonitor subMonitor = SubMonitor.convert(monitor, "Delete bookmark markers", bookmarks.size());
				for (Bookmark bookmark : bookmarks) {
					bookmarksMarkers.deleteMarker(bookmark.getId(), subMonitor.newChild(1));
				}
			});
		} catch (InvocationTargetException e) {
			StatusHelper.showError("Could not delete bookmark marker", e.getCause(), false);
		} catch (InterruptedException e) {
			throw new ExecutionException("Could not delete bookmark marker : cancelled");
		}
		return null;
	}


}
