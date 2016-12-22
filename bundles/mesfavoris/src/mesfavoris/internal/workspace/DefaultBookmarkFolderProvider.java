package mesfavoris.internal.workspace;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPage;

import com.google.common.collect.Lists;

import mesfavoris.BookmarksException;
import mesfavoris.internal.StatusHelper;
import mesfavoris.internal.views.BookmarksView;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;

/**
 * Provides the default bookmark folder where to add a new bookmark.
 * 
 * If bookmarks view is opened, the selected bookmark is used to determine the
 * bookmark folder, otherwise the "default" bookmark folder is returned.
 * 
 * @author cchabanois
 *
 */
public class DefaultBookmarkFolderProvider {
	public final static BookmarkId DEFAULT_BOOKMARKFOLDER_ID = new BookmarkId("default");
	private final BookmarkDatabase bookmarkDatabase;

	public DefaultBookmarkFolderProvider(BookmarkDatabase bookmarkDatabase) {
		this.bookmarkDatabase = bookmarkDatabase;
	}

	public BookmarkId getDefaultBookmarkFolder(IWorkbenchPage workbenchPage) {
		BookmarkFolder bookmarkFolder = getCurrentBookmarkFolderFromBookmarksView(workbenchPage);
		if (bookmarkFolder == null || !isModifiable(bookmarkFolder.getId())) {
			bookmarkFolder = (BookmarkFolder) bookmarkDatabase.getBookmarksTree()
					.getBookmark(DEFAULT_BOOKMARKFOLDER_ID);
		}
		if (bookmarkFolder == null || !isModifiable(bookmarkFolder.getId())) {
			createDefaultBookmarkFolder();
			bookmarkFolder = (BookmarkFolder) bookmarkDatabase.getBookmarksTree()
					.getBookmark(DEFAULT_BOOKMARKFOLDER_ID);
		}
		if (bookmarkFolder == null || !isModifiable(bookmarkFolder.getId())) {
			bookmarkFolder = bookmarkDatabase.getBookmarksTree().getRootFolder();
		}
		return bookmarkFolder.getId();
	}

	private void createDefaultBookmarkFolder() {
		try {
			bookmarkDatabase.modify(bookmarksTreeModifier -> {
				BookmarkFolder bookmarkFolder = new BookmarkFolder(DEFAULT_BOOKMARKFOLDER_ID, "default");
				bookmarksTreeModifier.addBookmarksAfter(bookmarksTreeModifier.getCurrentTree().getRootFolder().getId(),
						null, Lists.newArrayList(bookmarkFolder));
			});
		} catch (BookmarksException e) {
			StatusHelper.logWarn("Could not create default folder", e);
		}
	}

	private boolean isModifiable(BookmarkId bookmarkId) {
		return bookmarkDatabase.getBookmarksModificationValidator()
				.validateModification(bookmarkDatabase.getBookmarksTree(), bookmarkId).isOK();
	}

	private BookmarkFolder getCurrentBookmarkFolderFromBookmarksView(IWorkbenchPage page) {
		Object firstSelectedElement = getBookmarksViewSelection(page).getFirstElement();
		if (firstSelectedElement instanceof BookmarkFolder) {
			BookmarkFolder bookmarkFolder = (BookmarkFolder) firstSelectedElement;
			return bookmarkFolder;
		} else if (firstSelectedElement instanceof Bookmark) {
			Bookmark bookmark = (Bookmark) firstSelectedElement;
			return bookmarkDatabase.getBookmarksTree().getParentBookmark(bookmark.getId());
		}
		return null;
	}

	private IStructuredSelection getBookmarksViewSelection(IWorkbenchPage page) {
		BookmarksView bookmarksView = (BookmarksView) page.findView(BookmarksView.ID);
		if (bookmarksView == null) {
			return new StructuredSelection();
		}
		IStructuredSelection[] selection = new IStructuredSelection[1];
		page.getWorkbenchWindow().getShell().getDisplay()
				.syncExec(() -> selection[0] = (IStructuredSelection) bookmarksView.getSite().getSelectionProvider()
						.getSelection());
		return selection[0];
	}

}
