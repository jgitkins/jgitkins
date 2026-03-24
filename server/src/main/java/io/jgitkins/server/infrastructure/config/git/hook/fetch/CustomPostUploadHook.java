package io.jgitkins.server.infrastructure.config.git.hook.fetch;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.storage.pack.PackStatistics;
import org.eclipse.jgit.transport.PostUploadHook;

/**
 * PostUploadHook that prints PackStatistics for fetch operations.
 */
@Slf4j
public class CustomPostUploadHook implements PostUploadHook {
    @Override
    public void onPostUpload(PackStatistics packStatistics) {
        log.info("$$$ PostUploadHook.onPostUpload(): pack: reusedObjects={}, reusedDeltas={}, timeCounting(ms)={}, timeCompressing(ms)={}, timeWriting(ms)={}",
                packStatistics.getReusedObjects(),
                packStatistics.getReusedDeltas(),
                packStatistics.getTimeCounting(),
                packStatistics.getTimeCompressing(),
                packStatistics.getTimeWriting()
        );
    }
}
