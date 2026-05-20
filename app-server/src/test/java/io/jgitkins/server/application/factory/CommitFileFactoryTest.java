package io.jgitkins.server.application.factory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.FileUploadInfo;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.common.factory.CommitFileFactory;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class CommitFileFactoryTest {

    private final CommitFileFactory commitFileFactory = new CommitFileFactory();

    @Test
    void prepareUploadFile_mapsIoFailureToInfrastructureError() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        FileUploadInfo request = new FileUploadInfo();
        request.setFilePath("README.md");

        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenThrow(new IOException("read failed"));

        assertThatThrownBy(() -> commitFileFactory.prepareUploadFile(file, request))
                .isInstanceOf(JgitkinsException.class)
                .extracting(ex -> ((JgitkinsException) ex).getErrorCode())
                .isEqualTo(InfrastructureErrorCode.FILESYSTEM_ACCESS_FAILED);
    }
}
