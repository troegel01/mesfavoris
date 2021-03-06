package mesfavoris.persistence.json;

import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_BOOKMARKS;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_CHILDREN;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_ID;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_PROPERTIES;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_VERSION;
import static mesfavoris.persistence.json.JsonSerializerConstants.VERSION_1_0;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.google.gson.stream.JsonReader;

import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;

/**
 * Deserialize a {@link BookmarksTree}
 * 
 * @author cchabanois
 *
 */
public class BookmarksTreeJsonDeserializer implements IBookmarksTreeDeserializer {

	@Override
	public BookmarksTree deserialize(Reader reader, IProgressMonitor monitor) throws IOException {
		JsonReader jsonReader = new JsonReader(reader);
		try {
			return deserialize(jsonReader, monitor);
		} finally {
			jsonReader.close();
		}
	}

	private BookmarksTree deserialize(JsonReader reader, IProgressMonitor monitor) throws IOException {
		reader.beginObject();
		BookmarksTree bookmarksTree = null;
		if (!NAME_VERSION.equals(reader.nextName())) {
			throw new IOException("Invalid format");
		}
		if (!VERSION_1_0.equals(reader.nextString())) {
			throw new IOException("Invalid format : unknown version");
		}
		if (!NAME_BOOKMARKS.equals(reader.nextName())) {
			throw new IOException("Invalid format");
		}
		bookmarksTree = deserializeBookmarksTree(reader, monitor);
		reader.endObject();
		return bookmarksTree;
	}

	private BookmarksTree deserializeBookmarksTree(JsonReader reader, IProgressMonitor monitor) throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		reader.beginObject();
		BookmarkId id = null;
		Map<String, String> properties = Collections.emptyMap();
		boolean isFolder = false;

		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(NAME_ID)) {
				id = new BookmarkId(reader.nextString());
			} else if (name.equals(NAME_CHILDREN)) {
				isFolder = true;
				break;
			} else if (name.equals(NAME_PROPERTIES)) {
				properties = deserializeProperties(reader, subMonitor.split(1));
			} else {
				reader.skipValue();
			}
		}
		if (!isFolder || id == null) {
			throw new IOException("Invalid format");
		}
		BookmarkFolder bookmarkFolder = new BookmarkFolder(id, properties);
		BookmarksTree bookmarksTree = new BookmarksTree(bookmarkFolder);
		bookmarksTree = deserializeBookmarksArray(reader, bookmarksTree, bookmarkFolder.getId(), subMonitor.split(99));
		reader.endObject();
		return bookmarksTree;
	}

	private BookmarksTree deserializeBookmark(JsonReader reader, BookmarksTree bookmarksTree, BookmarkId parentId,
			IProgressMonitor monitor) throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		reader.beginObject();
		BookmarkId id = null;
		Map<String, String> properties = Collections.emptyMap();
		boolean isFolder = false;

		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(NAME_ID)) {
				id = new BookmarkId(reader.nextString());
			} else if (name.equals(NAME_CHILDREN)) {
				isFolder = true;
				break;
			} else if (name.equals(NAME_PROPERTIES)) {
				properties = deserializeProperties(reader, subMonitor.split(50));
			} else {
				reader.skipValue();
			}
		}
		if (isFolder) {
			BookmarkFolder bookmarkFolder = new BookmarkFolder(id, properties);
			bookmarksTree = bookmarksTree.addBookmarks(parentId, Arrays.<Bookmark> asList(bookmarkFolder));
			bookmarksTree = deserializeBookmarksArray(reader, bookmarksTree, bookmarkFolder.getId(), subMonitor.split(50));
		} else {
			Bookmark bookmark = new Bookmark(id, properties);
			bookmarksTree = bookmarksTree.addBookmarks(parentId, Arrays.asList(bookmark));
		}
		reader.endObject();
		return bookmarksTree;
	}

	private BookmarksTree deserializeBookmarksArray(JsonReader reader, BookmarksTree bookmarksTree, BookmarkId parentId,
			IProgressMonitor monitor) throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		reader.beginArray();
		while (reader.hasNext()) {
			bookmarksTree = deserializeBookmark(reader, bookmarksTree, parentId, subMonitor.setWorkRemaining(100).split(1));
		}
		reader.endArray();
		return bookmarksTree;
	}

	private Map<String, String> deserializeProperties(JsonReader reader, IProgressMonitor monitor) throws IOException {
		Map<String, String> properties = new TreeMap<String, String>();
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			String value = reader.nextString();
			properties.put(name, value);
		}
		reader.endObject();
		return properties;
	}

}
