package mesfavoris.internal.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

import mesfavoris.internal.BookmarksPlugin;
import mesfavoris.internal.views.properties.BookmarkPropertySource;
import mesfavoris.internal.views.virtual.BookmarkLink;
import mesfavoris.model.Bookmark;

public class BookmarkAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof Bookmark) {
			Bookmark bookmark = (Bookmark) adaptableObject;
			if (IPropertySource.class.equals(adapterType)) {
				return new BookmarkPropertySource(BookmarksPlugin.getDefault().getBookmarkDatabase(),
						BookmarksPlugin.getDefault().getBookmarkProblems(),
						BookmarksPlugin.getDefault().getBookmarkProblemDescriptors(), bookmark.getId());
			}
		}
		if (adaptableObject instanceof BookmarkLink) {
			Bookmark bookmark = ((BookmarkLink) adaptableObject).getBookmark();
			if (IPropertySource.class.equals(adapterType)) {
				return new BookmarkPropertySource(BookmarksPlugin.getDefault().getBookmarkDatabase(),
						BookmarksPlugin.getDefault().getBookmarkProblems(),
						BookmarksPlugin.getDefault().getBookmarkProblemDescriptors(), bookmark.getId());
			}
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}

}
