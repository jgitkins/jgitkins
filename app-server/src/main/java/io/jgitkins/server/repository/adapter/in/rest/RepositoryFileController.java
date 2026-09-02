package io.jgitkins.server.repository.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.jgitkins.server.repository.application.contract.FileEntry;
import io.jgitkins.server.repository.adapter.in.rest.contract.response.FileIndexEntry;
import io.jgitkins.server.repository.application.port.in.FileLoadUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Files", description = "저장소 파일 조회")
@RequestMapping("/repositories/{namespace}/{repoName}/files")
public class RepositoryFileController {

    private final FileLoadUseCase fileLoadUseCase;

    /**
     * Nullable on purpose. These are reads, and a public repository is readable anonymously; the
     * visibility decision belongs to canRead, not to this adapter. Demanding a principal here would
     * break every anonymous browse of a public repository.
     */
    private static Long optionalRequester(AuthenticatedUser currentUser) {
        return AuthenticatedUser.userIdOrNull(currentUser);
    }

    @Operation(summary = "List Repository Files", description = "지정한 참조(브랜치/커밋)의 전체 파일 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileEntry>>> listFiles(@PathVariable String namespace,
                                                                  @PathVariable String repoName,
                                                                  @RequestParam(name = "ref", required = false, defaultValue = "") String ref,
                                                                  @CurrentUser AuthenticatedUser currentUser) {

        List<FileEntry> files = fileLoadUseCase.getAllFiles(namespace, repoName, ref, optionalRequester(currentUser));
        return ApiResponse.ok(files);
    }

    @Operation(summary = "List Repository File Index", description = "Find a file 용 최소 인덱스(name/path/type) 조회")
    @GetMapping("/index")
    public ResponseEntity<ApiResponse<List<FileIndexEntry>>> listFileIndex(@PathVariable String namespace,
                                                                            @PathVariable String repoName,
                                                                            @RequestParam(name = "ref", required = false, defaultValue = "") String ref,
                                                                            @CurrentUser AuthenticatedUser currentUser) {
        List<FileIndexEntry> files = fileLoadUseCase.getAllFiles(namespace, repoName, ref, optionalRequester(currentUser)).stream()
                .map(file -> new FileIndexEntry(file.getName(), file.getPath(), file.getType()))
                .toList();
        return ApiResponse.ok(files);
    }
}
