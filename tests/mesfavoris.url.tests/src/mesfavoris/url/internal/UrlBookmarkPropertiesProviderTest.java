package mesfavoris.url.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import mesfavoris.model.Bookmark;
import mesfavoris.url.UrlBookmarkProperties;

public class UrlBookmarkPropertiesProviderTest {
	private final UrlBookmarkPropertiesProvider provider = new UrlBookmarkPropertiesProvider();

	@Test
	public void testUrlBookmarkPropertiesProvider() throws Exception {
		assertTitleAndFavIcon("GitHub - cchabanois/mesfavoris: Bookmarks eclipse plugin", "github-32x32.png",
				"https://github.com/cchabanois/mesfavoris");
	}

	@Test
	public void testUrlIsUsedAsTitleWhenAuthenticationIsNeeded() throws IOException {
		String url = "https://docs.google.com/a/salesforce.com/file/d/0B97G1IRAgxIEanhJTmkyS0NFem8/edit";
		assertTitleAndFavIcon(url, "google-32x32.png", url);
	}

	private void assertTitleAndFavIcon(String expectedTitle, String expectedIcon, String url) throws IOException {
		Map<String, String> bookmarkProperties = new HashMap<>();
		provider.addBookmarkProperties(bookmarkProperties, null, new StructuredSelection(new URL(url)),
				new NullProgressMonitor());
		assertEquals(expectedTitle, bookmarkProperties.get(Bookmark.PROPERTY_NAME));
		assertEquals(expectedIcon == null ? null : getImageAsIconBase64(expectedIcon),
				bookmarkProperties.get(UrlBookmarkProperties.PROP_ICON));
	}

	private String getImageAsIconBase64(String resourceName) throws IOException {
		try (InputStream is = getClass().getResourceAsStream(resourceName)) {
			byte[] byteArray = ByteStreams.toByteArray(is);
			return Base64.getEncoder().encodeToString(byteArray);
		}
	}

}
