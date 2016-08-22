package mesfavoris.internal.operations;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.runtime.IStatus;

import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.validation.IBookmarkModificationValidator;

public class AddBookmarkFolderOperation {
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarkModificationValidator bookmarkModificationValidator;

	public AddBookmarkFolderOperation(BookmarkDatabase bookmarkDatabase,
			IBookmarkModificationValidator bookmarkModificationValidator) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.bookmarkModificationValidator = bookmarkModificationValidator;
	}

	public void addBookmarkFolder(BookmarkId parentFolderId, String folderName) throws BookmarksException {
		BookmarkId id = new BookmarkId(UUID.randomUUID().toString());
		BookmarkFolder bookmarkFolder = new BookmarkFolder(id, folderName);
		addBookmarkFolder(parentFolderId, bookmarkFolder);
	}

	private void addBookmarkFolder(final BookmarkId parentFolderId, final BookmarkFolder bookmarkFolder)
			throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			IStatus status = bookmarkModificationValidator.validateModification(bookmarksTreeModifier.getCurrentTree(),
					parentFolderId);
			if (!status.isOK()) {
				throw new BookmarksException(status);
			}
			Optional<BookmarkId> latestFolderBookmark = latestFolderBookmark(bookmarksTreeModifier.getCurrentTree(),
					parentFolderId);
			bookmarksTreeModifier.addBookmarksAfter(parentFolderId, latestFolderBookmark.orElse(null),
					Arrays.asList((Bookmark) bookmarkFolder));
		});
	}

	private Optional<BookmarkId> latestFolderBookmark(BookmarksTree bookmarksTree, BookmarkId parentFolderId) {
		return bookmarksTree.getChildren(parentFolderId).stream().filter(bookmark -> bookmark instanceof BookmarkFolder)
				.map(bookmark -> bookmark.getId()).reduce((a, b) -> b);
	}

}
