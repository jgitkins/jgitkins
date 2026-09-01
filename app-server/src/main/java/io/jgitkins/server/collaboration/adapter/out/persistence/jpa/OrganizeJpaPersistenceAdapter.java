package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistence;
import io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * The JPA half of the Organize reference slice.
 *
 * <p>Implements the same two ports as {@code OrganizePersistenceAdapter} and is selected in its
 * place when the capability selector is set to {@code jpa}. Neither is a {@code @Component}: the
 * selector configuration constructs exactly one of them, because two component-annotated
 * implementations of {@code OrganizeRepository} would be an ambiguity error rather than a choice.
 *
 * <p>Behaviour is held to the MyBatis implementation, not to what JPA makes convenient:
 *
 * <ul>
 *   <li>{@code path} is derived from the organization name, matching {@code OrganizeDomainMapper}.
 *       The column is {@code NOT NULL UNIQUE}, so a different derivation shows up as a constraint
 *       violation under one selector and not the other.
 *   <li>{@code lockByIdForMembershipMutation} goes through the {@code PESSIMISTIC_WRITE} query and
 *       throws {@code OrganizeNotFoundException} on a missing row, both matching MyBatis.
 *   <li>Every other failure is wrapped in {@code InfrastructureException} with
 *       {@code PERSISTENCE_OPERATION_FAILED}, so callers see one error contract regardless of which
 *       implementation is wired.
 * </ul>
 */
@RequiredArgsConstructor
public class OrganizeJpaPersistenceAdapter implements OrganizePersistence {

    private final OrganizeJpaRepository organizeJpaRepository;

    @Override
    public Organize save(Organize organize) {
        return persist(organize, "save organize");
    }

    @Override
    public Organize update(Organize organize) {
        return persist(organize, "update organize");
    }

    private Organize persist(Organize organize, String operation) {
        try {
            OrganizeJpaEntity saved = organizeJpaRepository.save(toEntity(organize));
            return toDomain(saved);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during " + operation, e);
        }
    }

    @Override
    public Optional<Organize> findById(OrganizeId organizeId) {
        try {
            if (organizeId == null) {
                return Optional.empty();
            }
            return organizeJpaRepository.findById(organizeId.getValue()).map(this::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize by id", e);
        }
    }

    @Override
    public Organize lockByIdForMembershipMutation(OrganizeId organizeId) {
        try {
            return organizeJpaRepository.findByIdForUpdate(organizeId.getValue())
                    .map(this::toDomain)
                    .orElseThrow(() -> new OrganizeNotFoundException(organizeId.getValue()));
        } catch (OrganizeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during lock organize for membership mutation", e);
        }
    }

    @Override
    public Optional<Organize> findByName(OrganizeName name) {
        try {
            if (name == null) {
                return Optional.empty();
            }
            return organizeJpaRepository.findByName(name.getValue()).map(this::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize by name", e);
        }
    }

    @Override
    public List<Organize> findAll() {
        try {
            return organizeJpaRepository.findAll().stream().map(this::toDomain).toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all organizes", e);
        }
    }

    @Override
    public void deleteById(OrganizeId organizeId) {
        try {
            if (organizeId == null) {
                return;
            }
            organizeJpaRepository.deleteById(organizeId.getValue());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete organize", e);
        }
    }

    private OrganizeJpaEntity toEntity(Organize organize) {
        String name = organize.getName().getValue();
        return new OrganizeJpaEntity(
                organize.getId() != null ? organize.getId().getValue() : null,
                name,
                // PATH mirrors NAME, exactly as OrganizeDomainMapper does for MyBatis.
                name,
                organize.getDescription(),
                organize.getOwnerId() != null ? organize.getOwnerId().getValue() : null,
                organize.getCreatedAt(),
                organize.getUpdatedAt());
    }

    private Organize toDomain(OrganizeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Organize.reconstruct(
                entity.getId() != null ? OrganizeId.of(entity.getId()) : null,
                OrganizeName.from(entity.getName()),
                entity.getDescription(),
                entity.getOwnerId() != null ? OrganizeOwnerId.of(entity.getOwnerId()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
