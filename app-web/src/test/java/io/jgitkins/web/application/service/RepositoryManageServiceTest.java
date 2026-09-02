package io.jgitkins.web.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.web.application.contract.BranchSummary;
import io.jgitkins.web.application.contract.RepositoryBranchCreateResult;
import io.jgitkins.web.application.contract.RepositoryFileUploadResult;
import io.jgitkins.web.application.contract.RepositorySummary;
import io.jgitkins.web.application.port.out.RepositoryPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class RepositoryManageServiceTest {

	@Mock
	private RepositoryPort repositoryPort;

	private RepositoryManageService service;

	@BeforeEach
	void setUp() {
		service = new RepositoryManageService(repositoryPort);
		Mockito.lenient().when(repositoryPort.fetchRepositories()).thenReturn(List.of(repository()));
	}

	@Test
	void createBranchByPath_returnsErrorWhenBranchNameInvalid() {
		RepositoryBranchCreateResult result = service.createBranchByPath("team", "demo", " ", "main");

		assertEquals("유효한 브랜치 이름을 입력해 주세요.", result.errorMessage());
	}

	@Test
	void createBranchByPath_callsPortWhenValid() {
		when(repositoryPort.createBranch(1L, "feature/x", "main"))
				.thenReturn(new RepositoryBranchCreateResult(new BranchSummary(1L, "feature/x", false, false, false), null));

		RepositoryBranchCreateResult result = service.createBranchByPath("team", "demo", "feature/x", "main");

		assertNull(result.errorMessage());
		verify(repositoryPort).createBranch(1L, "feature/x", "main");
	}

	@Test
	void uploadFileByPath_rejectsFileOverLimit() {
		byte[] content = new byte[6 * 1024 * 1024];
		MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", content);

		RepositoryFileUploadResult result = service.uploadFileByPath("team", "demo", "main", "big.txt", "add", file);

		assertEquals("파일 크기는 5MB 이하만 허용됩니다.", result.errorMessage());
	}

	@Test
	void uploadFileByPath_callsPortWhenValid() {
		MockMultipartFile file = new MockMultipartFile("file", "new.md", "text/markdown", "hello".getBytes());
		when(repositoryPort.uploadFile(any())).thenReturn(new RepositoryFileUploadResult(null));

		RepositoryFileUploadResult result = service.uploadFileByPath("team", "demo", "main", "docs/new.md", "add file", file);

		assertNull(result.errorMessage());
		verify(repositoryPort).uploadFile(any());
	}

	private RepositorySummary repository() {
		return new RepositorySummary(
				1L, "USER", "demo", "team/demo", "main", "PRIVATE", null,
				1L, null, null, null, null, null
		);
	}
}
