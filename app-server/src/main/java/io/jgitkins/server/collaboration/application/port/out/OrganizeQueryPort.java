package io.jgitkins.server.collaboration.application.port.out;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import java.util.Optional;

public interface OrganizeQueryPort {

    Optional<Organize> findById(OrganizeId organizeId);

    Optional<Organize> findByName(OrganizeName name);
}
