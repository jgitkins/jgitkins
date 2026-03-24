package io.jgitkins.server.application.factory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.FileUploadInfo;
import io.jgitkins.server.common.exception.JgitkinsException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class CommitFileFactoryTest {

    private final CommitFileFactory commitFileFactory = new CommitFileFactory();

    @Test
    void prepareUploadFile_mapsIoFailureToApplicationError() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        FileUploadInfo request = new FileUploadInfo();
        request.setFilePath("README.md");

        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenThrow(new IOException("read failed"));

        assertThatThrownBy(() -> commitFileFactory.prepareUploadFile(file, request))
                .isInstanceOf(JgitkinsException.class)
                .extracting(ex -> ((JgitkinsException) ex).getErrorCode())
                .isEqualTo(ApplicationErrorCode.FILE_READ_FAILED);
    }
}
