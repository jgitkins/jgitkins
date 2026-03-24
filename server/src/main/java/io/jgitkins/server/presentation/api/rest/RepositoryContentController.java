package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.FileUploadInfo;
import io.jgitkins.server.application.dto.FileUploadRequest;
import io.jgitkins.server.application.dto.RepositoryKey;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.application.port.in.FileUploadUseCase;
import io.jgitkins.server.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "File Upload")
    @PostMapping(value = "/{namespace}/{repoName}/files/{branch}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = FileUploadRequest.class), encoding = @Encoding(name = "request", contentType = "application/json")))
    public ResponseEntity<ApiResponse<String>> uploadFile(@PathVariable @NotBlank String namespace,
            @PathVariable @NotBlank String repoName,
            @PathVariable @NotBlank String branch,
            @Parameter(schema = @Schema(type = "string", format = "binary")) @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") FileUploadInfo request) {
        fileUploadUseCase.uploadFileToRepository(namespace, repoName, branch, file, request);
        return ApiResponse.ok("File uploaded and committed.");
    }

    @Operation(summary = "File Upload (Web Compat)")
    @PostMapping(value = "/{repositoryId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFileByRepositoryId(@PathVariable Long repositoryId,
            @RequestParam("branch") @NotBlank String branch,
            @RequestParam("path") @NotBlank String path,
            @RequestParam("message") @NotBlank String message,
            @Parameter(schema = @Schema(type = "string", format = "binary")) @RequestPart("file") MultipartFile file) {
        RepositoryKey key = resolveRepositoryKey(repositoryId);
        FileUploadInfo request = FileUploadInfo.builder()
                .filePath(path)
                .commitMessage(message)
                .build();
        fileUploadUseCase.uploadFileToRepository(key.namespace(), key.repoName(), branch, file, request);
        return ApiResponse.ok("File uploaded and committed.");
    }

    @Operation(summary = "View File Tree", description = "트리 조회")
    @GetMapping("/{namespace}/{repoName}/refs/{branch}/tree")
    public ResponseEntity<ApiResponse<List<FileEntry>>> getTree(@PathVariable String namespace,
            @PathVariable String repoName,
            @PathVariable String branch,
            @RequestParam(name = "dir", required = false, defaultValue = "") String dir) {
        List<FileEntry> files = fileTreeLoadUseCase.getTree(namespace, repoName, branch, dir);
        return ApiResponse.ok(files);
    }

    private RepositoryKey resolveRepositoryKey(Long repositoryId) {
        var repository = repositoryLoadUseCase.getRepository(repositoryId);
        RepositoryKey key = RepositoryKey.fromPath(repository.getClonePath());
        if (key == null) {
            key = RepositoryKey.fromPath(repository.getPath());
        }
        if (key == null) {
            throw new ApplicationException(
                    ApplicationErrorCode.REPOSITORY_NOT_FOUND,
                    "Repository path is invalid: " + repositoryId);
        }
        return key;
    }
}
