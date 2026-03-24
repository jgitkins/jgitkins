package io.jgitkins.web.infrastructure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.jgitkins.web.application.model.RepositoryKey;
import org.junit.jupiter.api.Test;

class PathUtilsTest {

	@Test
	void parseRepositoryKey_parsesClonePathWithGitSuffix() {
		RepositoryKey key = PathUtils.parseRepositoryKey("/team/backend/demo.git");

		assertEquals("team/backend", key.namespace());
		assertEquals("demo", key.repoName());
	}

	@Test
	void parseRepositoryKey_returnsNullForInvalidPath() {
		assertNull(PathUtils.parseRepositoryKey("demo"));
		assertNull(PathUtils.parseRepositoryKey("   "));
		assertNull(PathUtils.parseRepositoryKey(null));
	}

	@Test
	void resolveRepositoryKey_prefersClonePathThenFallsBackToPath() {
		RepositoryKey fromClone = PathUtils.resolveRepositoryKey("owner/repo.git", "fallback/repo");
		RepositoryKey fromPath = PathUtils.resolveRepositoryKey(null, "/owner2/repo2/");

		assertEquals("owner", fromClone.namespace());
		assertEquals("repo", fromClone.repoName());
		assertEquals("owner2", fromPath.namespace());
		assertEquals("repo2", fromPath.repoName());
	}

	@Test
	void lastSegment_handlesTrailingSlashAndEmptyInput() {
		assertEquals("repo", PathUtils.lastSegment("team/repo/"));
		assertEquals("team", PathUtils.lastSegment("team"));
		assertEquals("", PathUtils.lastSegment("/"));
		assertEquals("", PathUtils.lastSegment(null));
	}
}
