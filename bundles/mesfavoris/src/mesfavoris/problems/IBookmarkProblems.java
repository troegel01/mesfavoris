package mesfavoris.problems;

import java.util.Optional;
import java.util.Set;

import mesfavoris.model.BookmarkId;

public interface IBookmarkProblems {

	Optional<BookmarkProblem> getBookmarkProblem(BookmarkId bookmarkId, String problemType);

	Set<BookmarkProblem> getBookmarkProblems(BookmarkId bookmarkId);

	void delete(BookmarkProblem problem);

	void add(BookmarkProblem problem);

	int size();

}