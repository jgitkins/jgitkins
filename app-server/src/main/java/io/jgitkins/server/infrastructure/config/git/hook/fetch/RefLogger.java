package io.jgitkins.server.infrastructure.config.git.hook.fetch;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefFilter;

import java.util.Iterator;
import java.util.Map;

/**
 * Simple RefFilter that logs all references instead of filtering them.
 */
@Slf4j
public class RefLogger implements RefFilter {
    @Override
    public Map<String, Ref> filter(Map<String, Ref> refs) {
        Iterator<Map.Entry<String, Ref>> iterator = refs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Ref> entry = iterator.next();
            String refName = entry.getKey();
            Ref refValue = entry.getValue();
            log.info("refKey: [{}] || refValue: [{}]", refName, refValue);
        }
        return refs;
    }
}
