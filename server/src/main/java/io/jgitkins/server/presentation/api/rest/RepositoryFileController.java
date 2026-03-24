package io.jgitkins.server.presentation.api.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.FileIndexEntry;
import io.jgitkins.server.application.port.in.FileLoadUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Files", description = "저장소 파일 조회")
@RequestMapping("/repositories/{namespace}/{repoName}/files")
public class RepositoryFileController {

    private final FileLoadUseCase fileLoadUseCase;

    @Operation(summary = "List Repository Files", description = "지정한 참조(브랜치/커밋)의 전체 파일 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileEntry>>> listFiles(@PathVariable String namespace,
                                                                  @PathVariable String repoName,
                                                                  @RequestParam(name = "ref", required = false, defaultValue = "") String ref) {

        List<FileEntry> files = fileLoadUseCase.getAllFiles(namespace, repoName, ref);
        return ApiResponse.ok(files);
    }

    @Operation(summary = "List Repository File Index", description = "Find a file 용 최소 인덱스(name/path/type) 조회")
    @GetMapping("/index")
    public ResponseEntity<ApiResponse<List<FileIndexEntry>>> listFileIndex(@PathVariable String namespace,
                                                                            @PathVariable String repoName,
                                                                            @RequestParam(name = "ref", required = false, defaultValue = "") String ref) {
        List<FileIndexEntry> files = fileLoadUseCase.getAllFiles(namespace, repoName, ref).stream()
                .map(file -> new FileIndexEntry(file.getName(), file.getPath(), file.getType()))
                .toList();
        return ApiResponse.ok(files);
    }
}
