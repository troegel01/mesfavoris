package mesfavoris.internal.views.properties;

import static mesfavoris.problems.BookmarkProblem.TYPE_PLACEHOLDER_UNDEFINED;
import static mesfavoris.problems.BookmarkProblem.TYPE_PROPERTIES_MAY_UPDATE;
import static mesfavoris.problems.BookmarkProblem.TYPE_PROPERTIES_NEED_UPDATE;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import mesfavoris.BookmarksException;
import mesfavoris.internal.BookmarksPlugin;
import mesfavoris.internal.StatusHelper;
import mesfavoris.internal.bookmarktypes.extension.PluginBookmarkType;
import mesfavoris.internal.bookmarktypes.extension.PluginBookmarkTypes;
import mesfavoris.internal.views.properties.PropertyLabelProvider.PropertyIcon;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.problems.BookmarkProblem;
import mesfavoris.problems.IBookmarkProblemDescriptorProvider;
import mesfavoris.problems.IBookmarkProblems;

public class BookmarkPropertySource implements IPropertySource {
	private static final String CATEGORY_UNKNOWN = "unknown";
	private final BookmarkId bookmarkId;
	private final IBookmarkProblems bookmarkProblems;
	private final IBookmarkProblemDescriptorProvider bookmarkProblemDescriptorProvider;
	private final PluginBookmarkTypes pluginBookmarkTypes;
	private final BookmarkDatabase bookmarkDatabase;

	public BookmarkPropertySource(BookmarkDatabase bookmarkDatabase, IBookmarkProblems bookmarkProblems,
			IBookmarkProblemDescriptorProvider bookmarkProblemDescriptorProvider, BookmarkId bookmarkId) {
		this.bookmarkId = bookmarkId;
		this.bookmarkProblems = bookmarkProblems;
		this.bookmarkProblemDescriptorProvider = bookmarkProblemDescriptorProvider;
		this.pluginBookmarkTypes = BookmarksPlugin.getDefault().getPluginBookmarkTypes();
		this.bookmarkDatabase = bookmarkDatabase;
	}

	@Override
	public Object getEditableValue() {
		return bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
	}

	private String getCategory(String propertyName) {
		return getPluginBookmarkType(propertyName).map(pluginBookmarkType -> pluginBookmarkType.getName())
				.orElse(CATEGORY_UNKNOWN);
	}

	private Optional<PluginBookmarkType> getPluginBookmarkType(String propertyName) {
		return pluginBookmarkTypes.getBookmarkTypes().stream()
				.filter(pluginBookmarkType -> pluginBookmarkType.getPropertyDescriptor(propertyName) != null)
				.findFirst();
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
		List<IPropertyDescriptor> propertyDescriptors = bookmark.getProperties().keySet().stream()
				.map(propertyName -> getPropertyDescriptorFromBookmarkProperty(propertyName))
				.collect(Collectors.toList());
		Optional<BookmarkProblem> mayUpdateProblem = getBookmarkProblem(TYPE_PROPERTIES_MAY_UPDATE);
		if (mayUpdateProblem.isPresent()) {
			propertyDescriptors.addAll(mayUpdateProblem.get().getProperties().keySet().stream()
					.filter(propertyName -> bookmark.getPropertyValue(propertyName) == null)
					.map(propertyName -> getPropertyDescriptorFromProblemProperty(mayUpdateProblem.get(), propertyName))
					.collect(Collectors.toList()));
		}
		Optional<BookmarkProblem> needUpdateProblem = getBookmarkProblem(TYPE_PROPERTIES_NEED_UPDATE);
		if (needUpdateProblem.isPresent()) {
			propertyDescriptors.addAll(needUpdateProblem.get().getProperties().keySet().stream()
					.filter(propertyName -> bookmark.getPropertyValue(propertyName) == null)
					.map(propertyName -> getPropertyDescriptorFromProblemProperty(needUpdateProblem.get(),
							propertyName))
					.collect(Collectors.toList()));
		}
		return propertyDescriptors.toArray(new IPropertyDescriptor[0]);
	}

	private IPropertyDescriptor getPropertyDescriptorFromBookmarkProperty(String propertyName) {
		PropertyDescriptor propertyDescriptor = new PropertyDescriptor(propertyName, propertyName);
		if (hasPlaceholderUndefinedProblem(propertyName)) {
			propertyDescriptor = new PropertyDescriptor(propertyName, propertyName);
			propertyDescriptor.setLabelProvider(new PropertyLabelProvider(false, PropertyIcon.WARNING));
		} else if (hasPropertyMayUpdateProblem(propertyName)) {
			propertyDescriptor = new ObsoletePropertyPropertyDescriptor(
					getBookmarkProblem(TYPE_PROPERTIES_MAY_UPDATE).get(),
					bookmarkProblemDescriptorProvider.getBookmarkProblemDescriptor(TYPE_PROPERTIES_MAY_UPDATE),
					propertyName);
		} else if (hasPropertyNeedUpdateProblem(propertyName)) {
			propertyDescriptor = new ObsoletePropertyPropertyDescriptor(
					getBookmarkProblem(TYPE_PROPERTIES_NEED_UPDATE).get(),
					bookmarkProblemDescriptorProvider.getBookmarkProblemDescriptor(TYPE_PROPERTIES_NEED_UPDATE),
					propertyName);
		} else {
			propertyDescriptor = new PropertyDescriptor(propertyName, propertyName);
		}
		propertyDescriptor.setCategory(getCategory(propertyName));
		return propertyDescriptor;
	}

	private boolean hasPlaceholderUndefinedProblem(String propertyName) {
		Optional<BookmarkProblem> problem = getBookmarkProblem(TYPE_PLACEHOLDER_UNDEFINED);
		Optional<String> value = problem.map(bookmarkProblem -> bookmarkProblem.getProperties().get(propertyName));
		return value.isPresent();
	}

	private boolean hasPropertyMayUpdateProblem(String propertyName) {
		Optional<BookmarkProblem> problem = getBookmarkProblem(TYPE_PROPERTIES_MAY_UPDATE);
		Optional<String> value = problem.map(bookmarkProblem -> bookmarkProblem.getProperties().get(propertyName));
		return value.isPresent();
	}

	private boolean hasPropertyNeedUpdateProblem(String propertyName) {
		Optional<BookmarkProblem> problem = getBookmarkProblem(TYPE_PROPERTIES_NEED_UPDATE);
		Optional<String> value = problem.map(bookmarkProblem -> bookmarkProblem.getProperties().get(propertyName));
		return value.isPresent();
	}

	private IPropertyDescriptor getPropertyDescriptorFromProblemProperty(BookmarkProblem bookmarkProblem,
			String propertyName) {
		NewPropertyPropertyDescriptor propertyDescriptor = new NewPropertyPropertyDescriptor(bookmarkProblem,
				bookmarkProblemDescriptorProvider.getBookmarkProblemDescriptor(bookmarkProblem.getProblemType()),
				propertyName);
		propertyDescriptor.setCategory(getCategory(propertyName));
		return propertyDescriptor;
	}

	@Override
	public Object getPropertyValue(Object id) {
		Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
		String propertyName = (String) id;
		String propertyValue = bookmark.getPropertyValue(propertyName);
		if (propertyValue != null) {
			return getPropertyValueFromBookmark(propertyName);
		} else {
			return getPropertyValueFromProblem(propertyName);
		}

	}

	private Object getPropertyValueFromProblem(String propertyName) {
		Optional<String> updatedValue = getBookmarkProblem(TYPE_PROPERTIES_MAY_UPDATE)
				.map(bookmarkProblem -> bookmarkProblem.getProperties().get(propertyName));
		if (!updatedValue.isPresent()) {
			updatedValue = getBookmarkProblem(TYPE_PROPERTIES_NEED_UPDATE)
					.map(bookmarkProblem -> bookmarkProblem.getProperties().get(propertyName));
		}
		return new UpdatedPropertyValue(updatedValue.get());
	}

	private Object getPropertyValueFromBookmark(String propertyName) {
		Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
		String propertyValue = bookmark.getPropertyValue(propertyName);
		Optional<String> updatedValue = getBookmarkProblem(TYPE_PROPERTIES_MAY_UPDATE)
				.map(bookmarkProblem -> bookmarkProblem.getProperties().get(propertyName));
		if (!updatedValue.isPresent()) {
			updatedValue = getBookmarkProblem(TYPE_PROPERTIES_NEED_UPDATE)
					.map(bookmarkProblem -> bookmarkProblem.getProperties().get(propertyName));
		}
		if (updatedValue.isPresent()) {
			return new ObsoletePropertyPropertySource(propertyName, propertyValue, updatedValue.get());
		} else {
			return propertyValue;
		}
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {

	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		try {
			bookmarkDatabase.modify(bookmarksTreeModifier -> {
				String propertyName = (String) id;
				String propertyValue = (String) value;
				bookmarksTreeModifier.setPropertyValue(bookmarkId, propertyName, propertyValue);
			});
		} catch (BookmarksException e) {
			StatusHelper.logWarn("Could not set property value", e);
		}
	}

	private Optional<BookmarkProblem> getBookmarkProblem(String problemType) {
		return bookmarkProblems.getBookmarkProblem(bookmarkId, problemType);
	}

}
