package mesfavoris.gdrive.mappings;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.collect.ImmutableSet;

import mesfavoris.gdrive.Activator;
import mesfavoris.gdrive.StatusHelper;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarksModification;

/**
 * Store mappings between bookmark folders and remote files that are storing
 * them
 * 
 * @author cchabanois
 *
 */
public class BookmarkMappingsStore implements IBookmarksListener, IBookmarkMappings {
	private final IBookmarkMappingsPersister bookmarkMappingsPersister;
	private final Map<BookmarkId, BookmarkMapping> mappings = new ConcurrentHashMap<>();
	private final SaveJob saveJob = new SaveJob();
	private final ListenerList<IBookmarkMappingsListener> listenerList = new ListenerList<>();

	public BookmarkMappingsStore(IBookmarkMappingsPersister bookmarkMappingsPersister) {
		this.bookmarkMappingsPersister = bookmarkMappingsPersister;
	}

	public void add(BookmarkId bookmarkFolderId, String fileId,  Map<String, String> properties) {
		if (add(new BookmarkMapping(bookmarkFolderId, fileId, properties))) {
			fireMappingAdded(bookmarkFolderId);
			saveJob.schedule();
		}
	}

	private boolean add(BookmarkMapping bookmarkMapping) {
		return mappings.put(bookmarkMapping.getBookmarkFolderId(), bookmarkMapping) == null;
	}

	private void replace(BookmarkMapping bookmarkMapping) {
		mappings.replace(bookmarkMapping.getBookmarkFolderId(), bookmarkMapping);
	}

	public void update(String fileId, Map<String,String> properties) {
		Optional<BookmarkMapping> mapping = getMapping(fileId);
		if (!mapping.isPresent()) {
			return;
		}
		if (!properties.equals(mapping.get().getProperties())) {
			replace(new BookmarkMapping(mapping.get().getBookmarkFolderId(), mapping.get().getFileId(), properties));
			saveJob.schedule();
		}
	}

	public Optional<BookmarkMapping> getMapping(BookmarkId bookmarkFolderId) {
		return mappings.values().stream().filter(mapping -> mapping.getBookmarkFolderId().equals(bookmarkFolderId))
				.findAny();
	}

	public Optional<BookmarkMapping> getMapping(String fileId) {
		return mappings.values().stream().filter(mapping -> mapping.getFileId().equals(fileId)).findAny();
	}

	public Set<BookmarkMapping> getMappings() {
		return ImmutableSet.copyOf(mappings.values());
	}

	public void remove(BookmarkId bookmarkFolderId) {
		if (mappings.remove(bookmarkFolderId) != null) {
			fireMappingRemoved(bookmarkFolderId);
			saveJob.schedule();
		}
	}

	public void init() {
		try {
			mappings.clear();
			bookmarkMappingsPersister.load().forEach(mapping -> add(mapping));
		} catch (IOException e) {
			StatusHelper.logError("Could not load bookmark mappings", e);
		}
	}

	public void close() throws InterruptedException {
		saveJob.join();
	}

	@Override
	public void bookmarksModified(List<BookmarksModification> modifications) {
		Set<BookmarkMapping> mappingsToRemove = modifications.stream()
				.filter(modification -> modification instanceof BookmarkDeletedModification)
				.map(modification -> (BookmarkDeletedModification) modification)
				.map(modification -> getDeletedMappings(modification))
				.reduce(new HashSet<BookmarkMapping>(), (mappingsSet, modificationMappingsSet) -> {
					mappingsSet.addAll(modificationMappingsSet);
					return mappingsSet;
				});
		mappingsToRemove.forEach(mapping -> remove(mapping.getBookmarkFolderId()));
	}

	private Set<BookmarkMapping> getDeletedMappings(BookmarkDeletedModification modification) {
		return mappings.values().stream()
				.filter(mapping -> modification.getTargetTree().getBookmark(mapping.getBookmarkFolderId()) == null)
				.collect(Collectors.toSet());
	}

	public void addListener(IBookmarkMappingsListener listener) {
		listenerList.add(listener);
	}

	public void removeListener(IBookmarkMappingsListener listener) {
		listenerList.remove(listener);
	}

	private void fireMappingAdded(BookmarkId bookmarkFolderId) {
		Object[] listeners = listenerList.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			final IBookmarkMappingsListener listener = (IBookmarkMappingsListener) listeners[i];
			SafeRunner.run(new ISafeRunnable() {

				public void run() throws Exception {
					listener.mappingAdded(bookmarkFolderId);

				}

				public void handleException(Throwable exception) {
					StatusHelper.logError("Error when mapping added", exception);
				}
			});
		}
	}

	private void fireMappingRemoved(BookmarkId bookmarkFolderId) {
		for (IBookmarkMappingsListener listener : listenerList) {
			SafeRunner.run(new ISafeRunnable() {

				public void run() throws Exception {
					listener.mappingRemoved(bookmarkFolderId);

				}

				public void handleException(Throwable exception) {
					StatusHelper.logError("Error when mapping removed", exception);
				}
			});
		}
	}

	private class SaveJob extends Job {

		public SaveJob() {
			super("Save bookmark mappings");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				bookmarkMappingsPersister.save(new HashSet<>(mappings.values()), monitor);
				return Status.OK_STATUS;
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Could not save Google Drive bookmarks store", e);
			} finally {
				monitor.done();
			}
		}

	}

}
