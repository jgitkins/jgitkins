package io.jgitkins.server.infrastructure.config.git.hook.fetch;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevObject;

import java.io.IOException;
import java.util.Collection;

import static org.eclipse.jgit.lib.Constants.typeString;


@Slf4j
public final class PackCompositionLogger {

    public static void logPackComposition(Repository repository,
                                          Collection<? extends ObjectId> wants,
                                          Collection<? extends ObjectId> haves) {
        if (wants == null || wants.isEmpty()) {
            log.info("[pack] skip composition log because wants is empty");
            return;
        }

        try (ObjectWalk walk = new ObjectWalk(repository)) {
            for (ObjectId want : wants) {
                walk.markStart(walk.parseAny(want));
            }
            if (haves != null) {
                for (ObjectId have : haves) {
                    walk.markUninteresting(walk.parseAny(have));
                }
            }

            RevObject obj;
            while ((obj = walk.next()) != null) {
                log.info("[pack] include {} {}", typeString(obj.getType()), obj.getId().name());
            }
            while ((obj = walk.nextObject()) != null) {
                log.info("[pack] include {} {}", typeString(obj.getType()), obj.getId().name());
            }
        } catch (IOException e) {
            log.warn("[pack] failed to trace composition", e);
        }
    }
}
