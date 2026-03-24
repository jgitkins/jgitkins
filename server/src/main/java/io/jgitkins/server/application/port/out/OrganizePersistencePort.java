package io.jgitkins.server.application.port.out;

import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;

import java.util.List;
import java.util.Optional;

public interface OrganizePersistencePort {
    Organize save(Organize organize);

    Organize update(Organize organize);

    Optional<Organize> findById(OrganizeId organizeId);

    Optional<Organize> findByName(OrganizeName name);

    List<Organize> findAll();

    void deleteById(OrganizeId organizeId);
}
