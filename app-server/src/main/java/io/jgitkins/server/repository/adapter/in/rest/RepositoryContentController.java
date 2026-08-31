package io.jgitkins.server.repository.adapter.in.rest;

import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.repository.application.contract.result.FileEntry;
import io.jgitkins.server.repository.application.contract.command.FileUploadInfo;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.FileUploadRequest;
import io.jgitkins.server.repository.application.contract.internal.RepositoryKey;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.repository.application.port.in.FileUploadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Content", description = "저장소 파일 업로드 및 트리 조회")
@RequestMapping("/api/repositories")
public class RepositoryContentController {

    private final FileUploadUseCase fileUploadUseCase;
    private final FileTreeLoadUseCase fileTreeLoadUseCase;
    private final RepositoryLoadUseCase repositoryLoadUseCase;


    /**
     * Resolves the caller once, before any use case is touched.
     *
     * <p>A malformed principal must not reach the application layer: if it did, the first observable
     * effect of a broken credential would be a database read for whatever id was salvaged from it.
     */


    /**
     * The requester, or 401.
     *
     * <p>Rejected here rather than inside the use case: the first observable effect of an absent or
     * unusable credential must not be a database read for whatever id was salvaged from it.
     */
    private static Long requireRequester(AuthenticatedUser currentUser) {
        if (currentUser == null) {
            throw new UnauthenticatedException("Authentication required");
        }
        return currentUser.userId();
    }

    @Operation(summary = "File Upload")
    @PostMapping(value = "/{namespace}/{repoName}/files/{branch}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = FileUploadRequest.class), encoding = @Encoding(name = "request", contentType = "application/json")))
    public ResponseEntity<ApiResponse<String>> uploadFile(@PathVariable @NotBlank String namespace,
            @PathVariable @NotBlank String repoName,
            @PathVariable @NotBlank String branch,
            @Parameter(schema = @Schema(type = "string", format = "binary")) @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") FileUploadInfo request,
            @CurrentUser AuthenticatedUser currentUser) {
        fileUploadUseCase.uploadFileToRepository(
                requireRequester(currentUser), namespace, repoName, branch, file, request);
        return ApiResponse.ok("File uploaded and committed.");
    }

    @Operation(summary = "File Upload (Web Compat)")
    @PostMapping(value = "/{repositoryId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFileByRepositoryId(@PathVariable @Positive Long repositoryId,
            @RequestParam("branch") @NotBlank String branch,
            @RequestParam("path") @NotBlank String path,
            @RequestParam("message") @NotBlank String message,
            @Parameter(schema = @Schema(type = "string", format = "binary")) @RequestPart("file") MultipartFile file,
            @CurrentUser AuthenticatedUser currentUser) {
        // The requester is resolved before the repository lookup, so an unauthenticated caller
        // cannot use this route to learn whether a repository id exists.
        Long requesterUserId = requireRequester(currentUser);
        RepositoryKey key = repositoryLoadUseCase.resolveRepositoryKey(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        FileUploadInfo request = FileUploadInfo.builder()
                .filePath(path)
                .commitMessage(message)
                .build();
        fileUploadUseCase.uploadFileToRepository(
                requesterUserId, key.namespace(), key.repoName(), branch, file, request);
        return ApiResponse.ok("File uploaded and committed.");
    }

    @Operation(summary = "View File Tree", description = "트리 조회")
    @GetMapping("/{namespace}/{repoName}/refs/{branch}/tree")
    public ResponseEntity<ApiResponse<List<FileEntry>>> getTree(@PathVariable String namespace,
            @PathVariable String repoName,
            @PathVariable String branch,
            @RequestParam(name = "dir", required = false, defaultValue = "") String dir,
            @CurrentUser AuthenticatedUser currentUser) {
        // Nullable requester: a public repository's tree is readable anonymously, and canRead decides.
        List<FileEntry> files = fileTreeLoadUseCase.getTree(namespace, repoName, branch, dir,
                AuthenticatedUser.userIdOrNull(currentUser));
        return ApiResponse.ok(files);
    }

    // TODO: 수정 필요
}
