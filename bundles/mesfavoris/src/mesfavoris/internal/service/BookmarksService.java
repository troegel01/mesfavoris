package mesfavoris.internal.service;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import mesfavoris.BookmarksException;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.bookmarktype.IBookmarkPropertyDescriptors;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.bookmarktype.NonUpdatablePropertiesProvider;
import mesfavoris.internal.numberedbookmarks.BookmarkNumber;
import mesfavoris.internal.numberedbookmarks.NumberedBookmarks;
import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.internal.service.operations.AddBookmarkFolderOperation;
import mesfavoris.internal.service.operations.AddBookmarkOperation;
import mesfavoris.internal.service.operations.AddBookmarksTreeOperation;
import mesfavoris.internal.service.operations.AddNumberedBookmarkOperation;
import mesfavoris.internal.service.operations.AddToRemoteBookmarksStoreOperation;
import mesfavoris.internal.service.operations.CheckBookmarkPropertiesOperation;
import mesfavoris.internal.service.operations.ConnectToRemoteBookmarksStoreOperation;
import mesfavoris.internal.service.operations.CopyBookmarkOperation;
import mesfavoris.internal.service.operations.CutBookmarkOperation;
import mesfavoris.internal.service.operations.DeleteBookmarksOperation;
import mesfavoris.internal.service.operations.GetLinkedBookmarksOperation;
import mesfavoris.internal.service.operations.GotoBookmarkOperation;
import mesfavoris.internal.service.operations.GotoNumberedBookmarkOperation;
import mesfavoris.internal.service.operations.PasteBookmarkOperation;
import mesfavoris.internal.service.operations.RefreshRemoteFolderOperation;
import mesfavoris.internal.service.operations.RemoveFromRemoteBookmarksStoreOperation;
import mesfavoris.internal.service.operations.RenameBookmarkOperation;
import mesfavoris.internal.service.operations.SetBookmarkCommentOperation;
import mesfavoris.internal.service.operations.ShowInBookmarksViewOperation;
import mesfavoris.internal.service.operations.SortByNameOperation;
import mesfavoris.internal.service.operations.UpdateBookmarkOperation;
import mesfavoris.internal.service.operations.utils.INewBookmarkPositionProvider;
import mesfavoris.markers.IBookmarksMarkers;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.placeholders.IPathPlaceholders;
import mesfavoris.problems.IBookmarkProblems;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.service.IBookmarksService;

public class BookmarksService implements IBookmarksService {
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarkPropertiesProvider bookmarkPropertiesProvider;
	private final INewBookmarkPositionProvider newBookmarkPositionProvider;
	private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;
	private final IBookmarksDirtyStateTracker bookmarksDirtyStateTracker;
	private final IBookmarkLocationProvider bookmarkLocationProvider;
	private final IGotoBookmark gotoBookmark;
	private final NumberedBookmarks numberedBookmarks;
	private final IBookmarkPropertyDescriptors bookmarkPropertyDescriptors;
	private final IBookmarksMarkers bookmarksMarkers;
	private final IPathPlaceholders pathPlaceholders;
	private final IBookmarkProblems bookmarkProblems;
	private final IEventBroker eventBroker;
	private final IPathPlaceholderResolver pathPlaceholderResolver;

	public BookmarksService(BookmarkDatabase bookmarkDatabase, IBookmarkPropertiesProvider bookmarkPropertiesProvider,
			INewBookmarkPositionProvider newBookmarkPositionProvider,
			RemoteBookmarksStoreManager remoteBookmarksStoreManager,
			IBookmarksDirtyStateTracker bookmarksDirtyStateTracker, IBookmarkLocationProvider bookmarkLocationProvider,
			IGotoBookmark gotoBookmark, NumberedBookmarks numberedBookmarks,
			IBookmarkPropertyDescriptors bookmarkPropertyDescriptors, IBookmarksMarkers bookmarksMarkers,
			IPathPlaceholders pathPlaceholders, IBookmarkProblems bookmarkProblems, IEventBroker eventBroker) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.bookmarkPropertiesProvider = bookmarkPropertiesProvider;
		this.newBookmarkPositionProvider = newBookmarkPositionProvider;
		this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
		this.bookmarksDirtyStateTracker = bookmarksDirtyStateTracker;
		this.bookmarkLocationProvider = bookmarkLocationProvider;
		this.gotoBookmark = gotoBookmark;
		this.numberedBookmarks = numberedBookmarks;
		this.bookmarkPropertyDescriptors = bookmarkPropertyDescriptors;
		this.bookmarksMarkers = bookmarksMarkers;
		this.pathPlaceholders = pathPlaceholders;
		this.bookmarkProblems = bookmarkProblems;
		this.eventBroker = eventBroker;
		this.pathPlaceholderResolver = new PathPlaceholderResolver(pathPlaceholders);
	}

	@Override
	public BookmarksTree getBookmarksTree() {
		return bookmarkDatabase.getBookmarksTree();
	}

	@Override
	public void addBookmarkFolder(BookmarkId parentFolderId, String folderName) throws BookmarksException {
		AddBookmarkFolderOperation operation = new AddBookmarkFolderOperation(bookmarkDatabase);
		operation.addBookmarkFolder(parentFolderId, folderName);
	}

	@Override
	public BookmarkId addBookmark(IWorkbenchPart part, ISelection selection, IProgressMonitor monitor)
			throws BookmarksException {
		AddBookmarkOperation operation = new AddBookmarkOperation(bookmarkDatabase, bookmarkPropertiesProvider,
				newBookmarkPositionProvider);
		return operation.addBookmark(part, selection, monitor);
	}

	@Override
	public void addBookmarksTree(BookmarkId parentBookmarkId, BookmarksTree sourceBookmarksTree,
			Consumer<BookmarksTree> afterCommit) throws BookmarksException {
		AddBookmarksTreeOperation operation = new AddBookmarksTreeOperation(bookmarkDatabase);
		operation.addBookmarksTree(parentBookmarkId, sourceBookmarksTree, afterCommit);
	}

	@Override
	public void addToRemoteBookmarksStore(String storeId, final BookmarkId bookmarkFolderId,
			final IProgressMonitor monitor) throws BookmarksException {
		AddToRemoteBookmarksStoreOperation operation = new AddToRemoteBookmarksStoreOperation(bookmarkDatabase,
				remoteBookmarksStoreManager);
		operation.addToRemoteBookmarksStore(storeId, bookmarkFolderId, monitor);
	}

	@Override
	public void connectToRemoteBookmarksStore(String storeId, IProgressMonitor monitor) throws BookmarksException {
		ConnectToRemoteBookmarksStoreOperation operation = new ConnectToRemoteBookmarksStoreOperation(bookmarkDatabase,
				remoteBookmarksStoreManager, bookmarksDirtyStateTracker);
		operation.connect(storeId, monitor);
	}

	@Override
	public void disconnectFromRemoteBookmarksStore(String storeId, IProgressMonitor monitor) throws BookmarksException {
		ConnectToRemoteBookmarksStoreOperation operation = new ConnectToRemoteBookmarksStoreOperation(bookmarkDatabase,
				remoteBookmarksStoreManager, bookmarksDirtyStateTracker);
		operation.disconnect(storeId, monitor);
	}

	@Override
	public void copyToClipboard(List<BookmarkId> selection) {
		CopyBookmarkOperation operation = new CopyBookmarkOperation();
		operation.copyToClipboard(bookmarkDatabase.getBookmarksTree(), selection);
	}

	@Override
	public void cutToClipboard(List<BookmarkId> selection) throws BookmarksException {
		CutBookmarkOperation operation = new CutBookmarkOperation(bookmarkDatabase);
		operation.cutToClipboard(selection);
	}

	@Override
	public void deleteBookmarks(final List<BookmarkId> selection, boolean recurse) throws BookmarksException {
		DeleteBookmarksOperation operation = new DeleteBookmarksOperation(bookmarkDatabase);
		operation.deleteBookmarks(selection, recurse);
	}

	@Override
	public List<Bookmark> getLinkedBookmarks(IWorkbenchPart part, ISelection selection) {
		GetLinkedBookmarksOperation operation = new GetLinkedBookmarksOperation(bookmarkDatabase);
		return operation.getLinkedBookmarks(part, selection);
	}

	@Override
	public void paste(BookmarkId parentBookmarkId, IProgressMonitor monitor) throws BookmarksException {
		PasteBookmarkOperation operation = new PasteBookmarkOperation(bookmarkDatabase, bookmarkPropertiesProvider);
		operation.paste(parentBookmarkId, monitor);
	}

	@Override
	public void pasteAfter(BookmarkId parentBookmarkId, BookmarkId bookmarkId, IProgressMonitor monitor) throws BookmarksException {
		PasteBookmarkOperation operation = new PasteBookmarkOperation(bookmarkDatabase, bookmarkPropertiesProvider);
		operation.pasteAfter(parentBookmarkId, bookmarkId, monitor);
	}	
	
	@Override
	public void refresh(BookmarkId bookmarkFolderId, IProgressMonitor monitor) throws BookmarksException {
		RefreshRemoteFolderOperation operation = new RefreshRemoteFolderOperation(bookmarkDatabase,
				remoteBookmarksStoreManager, bookmarksDirtyStateTracker);
		operation.refresh(bookmarkFolderId, monitor);
	}

	@Override
	public void refresh(IProgressMonitor monitor) throws BookmarksException {
		RefreshRemoteFolderOperation operation = new RefreshRemoteFolderOperation(bookmarkDatabase,
				remoteBookmarksStoreManager, bookmarksDirtyStateTracker);
		operation.refresh(monitor);
	}

	@Override
	public void refresh(String storeId, IProgressMonitor monitor) throws BookmarksException {
		RefreshRemoteFolderOperation operation = new RefreshRemoteFolderOperation(bookmarkDatabase,
				remoteBookmarksStoreManager, bookmarksDirtyStateTracker);
		operation.refresh(storeId, monitor);
	}

	@Override
	public void removeFromRemoteBookmarksStore(String storeId, final BookmarkId bookmarkFolderId,
			final IProgressMonitor monitor) throws BookmarksException {
		RemoveFromRemoteBookmarksStoreOperation operation = new RemoveFromRemoteBookmarksStoreOperation(
				bookmarkDatabase, remoteBookmarksStoreManager);
		operation.removeFromRemoteBookmarksStore(storeId, bookmarkFolderId, monitor);
	}

	@Override
	public void renameBookmark(BookmarkId bookmarkId, String newName) throws BookmarksException {
		RenameBookmarkOperation operation = new RenameBookmarkOperation(bookmarkDatabase);
		operation.renameBookmark(bookmarkId, newName);
	}

	@Override
	public void setComment(final BookmarkId bookmarkId, final String comment) throws BookmarksException {
		SetBookmarkCommentOperation operation = new SetBookmarkCommentOperation(bookmarkDatabase);
		operation.setComment(bookmarkId, comment);
	}

	@Override
	public void showInBookmarksView(IWorkbenchPage page, BookmarkId bookmarkId, boolean activate) {
		ShowInBookmarksViewOperation operation = new ShowInBookmarksViewOperation(bookmarkDatabase);
		operation.showInBookmarksView(page, bookmarkId, activate);
	}

	@Override
	public void sortByName(BookmarkId bookmarkFolderId) throws BookmarksException {
		SortByNameOperation operation = new SortByNameOperation(bookmarkDatabase);
		operation.sortByName(bookmarkFolderId);
	}

	@Override
	public void updateBookmark(BookmarkId bookmarkId, IWorkbenchPart part, ISelection selection,
			IProgressMonitor monitor) throws BookmarksException {
		UpdateBookmarkOperation operation = new UpdateBookmarkOperation(bookmarkDatabase, bookmarkProblems,
				bookmarkPropertiesProvider, new NonUpdatablePropertiesProvider(bookmarkPropertyDescriptors));
		operation.updateBookmark(bookmarkId, part, selection, monitor);
	}

	private CheckBookmarkPropertiesOperation getCheckBookmarkPropertiesOperation() {
		return new CheckBookmarkPropertiesOperation(bookmarkDatabase, remoteBookmarksStoreManager,
				bookmarkPropertyDescriptors, bookmarkPropertiesProvider, pathPlaceholderResolver, bookmarkProblems);
	}

	private GotoBookmarkOperation getGotoBookmarkOperation() {
		return new GotoBookmarkOperation(bookmarkDatabase, bookmarkLocationProvider, gotoBookmark, bookmarksMarkers,
				bookmarkPropertiesProvider, getCheckBookmarkPropertiesOperation(), bookmarkProblems, eventBroker);
	}

	@Override
	public void gotoBookmark(BookmarkId bookmarkId, IProgressMonitor monitor) throws BookmarksException {
		GotoBookmarkOperation gotoBookmarkOperation = getGotoBookmarkOperation();
		gotoBookmarkOperation.gotoBookmark(bookmarkId, monitor);
	}

	@Override
	public void addNumberedBookmark(BookmarkId bookmarkId, BookmarkNumber bookmarkNumber) {
		AddNumberedBookmarkOperation operation = new AddNumberedBookmarkOperation(numberedBookmarks);
		operation.addNumberedBookmark(bookmarkId, bookmarkNumber);
	}

	@Override
	public void gotoNumberedBookmark(BookmarkNumber bookmarkNumber, IProgressMonitor monitor)
			throws BookmarksException {
		GotoBookmarkOperation gotoBookmarkOperation = getGotoBookmarkOperation();
		GotoNumberedBookmarkOperation operation = new GotoNumberedBookmarkOperation(numberedBookmarks, bookmarkDatabase,
				gotoBookmarkOperation);
		operation.gotoNumberedBookmark(bookmarkNumber, monitor);
	}

}
