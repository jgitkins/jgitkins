package io.jgitkins.server.infrastructure.config.git.hook.fetch;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.transport.PreUploadHook;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.UploadPack;

import java.io.IOException;
import java.util.Collection;

/**
 * Invoked When PackFile Retrieval Event
 */
@Slf4j
public class CustomPreUploadHook implements PreUploadHook {

    @Override
    public void onBeginNegotiateRound(UploadPack up, Collection<? extends ObjectId> wants, int cnt) {
        log.info("[v0/v1] $$$ PreUploadHook.onBeginNegotiateRound(): round, wants={}, havesSeen={}", wants.size(), cnt);
        log.info("Client's Wants List:");
        wants.forEach(oid -> log.info("wants: [{}]", oid.name()));
    }

    @Override
    public void onEndNegotiateRound(UploadPack uploadPack,
                                    Collection<? extends ObjectId> wants,
                                    int i,
                                    int i1,
                                    boolean b) throws ServiceMayNotContinueException {
        log.info("[v0/v1] $$$ PreUploadHook.onEndNegotiateRound(): wants: {}", wants);
    }

    @Override
    public void onSendPack(UploadPack uploadPack,
                           Collection<? extends ObjectId> wants,
                           Collection<? extends ObjectId> haves) throws ServiceMayNotContinueException {
        log.info("[v0/v1] $$$ PreUploadHook.onSendPack(): wants: [{}], haves: [{}]", wants, haves);
        try (ObjectReader reader = uploadPack.getRepository().newObjectReader()) {
            int commits = 0;
            int trees = 0;
            int blobs = 0;
            int tags = 0;
            int others = 0;
            for (ObjectId id : wants) {
                int type = reader.open(id).getType();
                switch (type) {
                    case org.eclipse.jgit.lib.Constants.OBJ_COMMIT:
                        commits++;
                        break;
                    case org.eclipse.jgit.lib.Constants.OBJ_TREE:
                        trees++;
                        break;
                    case org.eclipse.jgit.lib.Constants.OBJ_BLOB:
                        blobs++;
                        break;
                    case org.eclipse.jgit.lib.Constants.OBJ_TAG:
                        tags++;
                        break;
                    default:
                        others++;
                }
            }
            log.info("[v0/v1] sendPack(types on wants): commit=[{}], tree=[{}], blob=[{}], tag=[{}], others=[{}]",
                    commits, trees, blobs, tags, others);
        } catch (IOException e) {
            log.warn("fail to open object cause: ", e);
        }

        PackCompositionLogger.logPackComposition(uploadPack.getRepository(), wants, haves);
    }
}
