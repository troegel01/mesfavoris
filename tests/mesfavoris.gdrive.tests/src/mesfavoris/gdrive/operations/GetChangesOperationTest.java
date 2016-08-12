package mesfavoris.gdrive.operations;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.File;

import mesfavoris.gdrive.test.GDriveConnectionRule;

public class GetChangesOperationTest {

	@Rule
	public GDriveConnectionRule gdriveConnectionRule = new GDriveConnectionRule(true);

	private GetChangesOperation getChangesOperation;
	private Long startChangeId;

	@Before
	public void setUp() throws Exception {
		getChangesOperation = new GetChangesOperation(gdriveConnectionRule.getDrive());
		startChangeId = getChangesOperation.getLargestChangeId() + 1;
	}

	@Test
	public void testNoChanges() throws IOException {
		// Given

		// When
		List<Change> changes = getChangesOperation.getChanges(startChangeId);

		// Then
		assertEquals(0, changes.size());
	}

	@Test
	public void testFileAddedChange() throws IOException {
		// Given
		File file1 = createFile("file1.txt", "the contents");
		File file2 = createFile("file2.txt", "the contents");

		// When
		List<Change> changes = getChangesOperation.getChanges(startChangeId);

		// Then
		assertEquals(2, changes.size());
		assertEquals(file1.getId(), changes.get(0).getFileId());
		assertEquals(file2.getId(), changes.get(1).getFileId());
	}

	@Test
	public void testGetNextStartChangeId() throws IOException {
		// Given
		File file1 = createFile("file1.txt", "the contents");
		List<Change> changes = getChangesOperation.getChanges(startChangeId);

		// When
		startChangeId = changes.get(changes.size() - 1).getId()+1;
		File file2 = createFile("file2.txt", "the contents");
		changes = getChangesOperation.getChanges(startChangeId);

		// Then
		assertEquals(1, changes.size());
		assertEquals(file2.getId(), changes.get(0).getFileId());
	}

	private File createFile(String name, String content) throws IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(gdriveConnectionRule.getDrive());
		byte[] contents = content.getBytes("UTF-8");
		File file = createFileOperation.createFile(gdriveConnectionRule.getApplicationFolderId(), name, contents,
				new NullProgressMonitor());
		return file;
	}

}