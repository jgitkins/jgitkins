package io.jgitkins.runner.infrastructure.docker;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerBindBuilder {

    private final DockerPathInspector pathInspector;

    public List<Bind> build(String workspacePath, String pluginPath) {
        List<Bind> binds = new ArrayList<>();
        if (StringUtils.hasText(workspacePath)) {
            if (pathInspector.isReadableDirectory(workspacePath)) {
                binds.add(new Bind(workspacePath, new Volume("/workspace")));
            } else {
                log.warn("Workspace path is not readable: {}", workspacePath);
            }
        }
        if (StringUtils.hasText(pluginPath)) {
            if (pathInspector.isReadableFile(pluginPath)) {
                binds.add(new Bind(pluginPath, new Volume("/workspace/plugins.txt")));
            } else {
                log.warn("Plugin config file is not readable: {}", pluginPath);
            }
        }
        return binds;
    }
}
