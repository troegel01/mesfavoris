package mesfavoris.gdrive.operations;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.Charsets;
import com.google.api.services.drive.Drive.Revisions.List;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.RevisionList;

import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.test.GDriveConnectionRule;

public class UpdateFileOperationTest {

	@Rule
	public GDriveConnectionRule gdriveConnectionUser1 = new GDriveConnectionRule(GDriveTestUser.USER1, true);

	@Rule
	public GDriveConnectionRule gdriveConnectionUser2 = new GDriveConnectionRule(GDriveTestUser.USER2, true);

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testUpdateFile() throws Exception {
		// Given
		File file = createFile(gdriveConnectionUser1, "myFile.txt", "the contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive());

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		updateFileOperation.updateFile(file.getId(), "the new contents".getBytes(Charsets.UTF_8), null,
				new NullProgressMonitor());

		// Then
		assertEquals("the new contents", new String(downloadFile(gdriveConnectionUser1, file.getId()), Charsets.UTF_8));
	}

	@Test
	public void testUpdateFileDoesNotCreateNewRevisionIfModificationsAreCloseInTime() throws Exception {
		// Given
		File file = createFile(gdriveConnectionUser1, "myFile.txt", "original contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive(),
				Duration.ofSeconds(10));

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		updateFileOperation.updateFile(file.getId(), "first modification".getBytes(Charsets.UTF_8), null,
				new NullProgressMonitor());
		updateFileOperation.updateFile(file.getId(), "second modification".getBytes(Charsets.UTF_8), null,
				new NullProgressMonitor());

		// Then
		assertEquals("second modification",
				new String(downloadFile(gdriveConnectionUser1, file.getId()), Charsets.UTF_8));
		assertEquals(1, getRevisionsCount(gdriveConnectionUser1, file.getId()));
	}
	
	@Test
	public void testUpdateFileCreatesNewRevisionIfUserIsNotTheSameThanForPreviousModification() throws Exception {
		// Given
		File file = createFile(gdriveConnectionUser1, "myFile.txt", "original contents");
		share(gdriveConnectionUser1, file.getId(), GDriveTestUser.USER2.getEmail());
		UpdateFileOperation updateFileOperationUser1 = new UpdateFileOperation(gdriveConnectionUser1.getDrive(),
				Duration.ofSeconds(10));
		UpdateFileOperation updateFileOperationUser2 = new UpdateFileOperation(gdriveConnectionUser2.getDrive(),
				Duration.ofSeconds(10));

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		updateFileOperationUser1.updateFile(file.getId(), "first modification".getBytes(Charsets.UTF_8), null,
				new NullProgressMonitor());
		updateFileOperationUser2.updateFile(file.getId(), "second modification".getBytes(Charsets.UTF_8), null,
				new NullProgressMonitor());

		// Then
		assertEquals("second modification",
				new String(downloadFile(gdriveConnectionUser1, file.getId()), Charsets.UTF_8));
		assertEquals(2, getRevisionsCount(gdriveConnectionUser1, file.getId()));
	}

	@Test
	public void testUpdateFileCreatesNewRevisionIfModificationsAreNotCloseInTime() throws Exception {
		// Given
		File file = createFile(gdriveConnectionUser1, "myFile.txt", "original contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive(),
				Duration.ofSeconds(10));

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		updateFileOperation.updateFile(file.getId(), "first modification".getBytes(Charsets.UTF_8), null,
				new NullProgressMonitor());
		TimeUnit.SECONDS.sleep(14);
		updateFileOperation.updateFile(file.getId(), "second modification".getBytes(Charsets.UTF_8), null,
				new NullProgressMonitor());

		// Then
		assertEquals("second modification",
				new String(downloadFile(gdriveConnectionUser1, file.getId()), Charsets.UTF_8));
		assertEquals(2, getRevisionsCount(gdriveConnectionUser1, file.getId()));
	}

	@Test
	public void testCannotUpdateFileThatHasBeenUpdatedSince() throws Exception {
		// Given
		File file = createFile(gdriveConnectionUser1, "myFile.txt", "the contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive());
		updateFileOperation.updateFile(file.getId(), "the new contents".getBytes(Charsets.UTF_8), file.getEtag(),
				new NullProgressMonitor());
		exception.expect(GoogleJsonResponseException.class);
		exception.expectMessage("412 Precondition Failed");

		// When
		updateFileOperation.updateFile(file.getId(), "the newest contents".getBytes(Charsets.UTF_8), file.getEtag(),
				new NullProgressMonitor());

		// Then
	}

	private File createFile(GDriveConnectionRule connection, String name, String contents)
			throws UnsupportedEncodingException, IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(connection.getDrive());
		File file = createFileOperation.createFile(connection.getApplicationFolderId(), name,
				contents.getBytes("UTF-8"), new NullProgressMonitor());
		return file;
	}

	private byte[] downloadFile(GDriveConnectionRule connection, String fileId) throws IOException {
		DownloadFileOperation downloadFileOperation = new DownloadFileOperation(connection.getDrive());
		return downloadFileOperation.downloadFile(fileId, new NullProgressMonitor());
	}

	private int getRevisionsCount(GDriveConnectionRule connection, String fileId) throws IOException {
		List list = connection.getDrive().revisions().list(fileId);
		RevisionList revisionList = list.execute();
		return revisionList.getItems().size();
	}
	
	private void share(GDriveConnectionRule driveConnection, String fileId, String userEmail) throws IOException {
		ShareFileOperation shareFileOperation = new ShareFileOperation(driveConnection.getDrive());
		shareFileOperation.shareWithUser(fileId, userEmail, true);
	}

}
