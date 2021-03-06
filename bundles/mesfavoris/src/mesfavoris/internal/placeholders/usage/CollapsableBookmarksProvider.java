package mesfavoris.internal.placeholders.usage;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IPath;

import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.model.Bookmark;
import mesfavoris.placeholders.IPathPlaceholderResolver;

/**
 * Get the bookmarks for which path is collapsable using given placeholder
 * 
 * @author cchabanois
 *
 */
public class CollapsableBookmarksProvider {
	private final String pathPlaceholderName;
	private final IPathPlaceholderResolver pathPlaceholderResolver;
	private final List<String> pathPropertyNames;
	
	public CollapsableBookmarksProvider(IPathPlaceholderResolver pathPlaceholderResolver, List<String> pathPropertyNames, String pathPlaceholderName) {
		this.pathPlaceholderName = pathPlaceholderName;
		this.pathPlaceholderResolver = pathPlaceholderResolver;
		this.pathPropertyNames = pathPropertyNames;
	}

	public List<Bookmark> getCollapsableBookmarks(Iterable<Bookmark> bookmarks) {
		return StreamSupport.stream(bookmarks.spliterator(), false).filter(this::isCollapsable)
				.collect(Collectors.toList());
	}

	private boolean isCollapsable(Bookmark bookmark) {
		for (String pathPropertyName : pathPropertyNames) {
			if (isCollapsable(bookmark, pathPropertyName)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isCollapsable(Bookmark bookmark, String propertyName) {
		String filePath = bookmark.getPropertyValue(propertyName);
		if (filePath == null) {
			return false;
		}
		String variableName = PathPlaceholderResolver.getPlaceholderName(filePath);
		if (pathPlaceholderName.equals(variableName)) {
			return false;
		}
		IPath path = pathPlaceholderResolver.expand(filePath);
		if (path == null) {
			return false;
		}
		String collapsedPath = pathPlaceholderResolver.collapse(path, pathPlaceholderName);
		variableName = PathPlaceholderResolver.getPlaceholderName(collapsedPath);
		return pathPlaceholderName.equals(variableName);
	}

}
