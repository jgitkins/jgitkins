package io.jgitkins.server.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Schema(name = "FileUploadRequest")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    @Schema(type = "string", format = "binary", description = "업로드 파일")
    private MultipartFile file;

    @Schema(description = "메타데이터(JSON)")
    private FileUploadInfo request;
}
