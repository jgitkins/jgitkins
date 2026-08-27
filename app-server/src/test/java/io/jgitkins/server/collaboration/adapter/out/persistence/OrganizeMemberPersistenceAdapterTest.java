package io.jgitkins.server.collaboration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeMemberDomainMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.model.OrganizeMemberEntity;
import io.jgitkins.server.collaboration.adapter.out.persistence.model.OrganizeMemberEntityCondition;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeMemberPersistenceAdapterTest {

    @Mock
    private OrganizeMemberEntityMbgMapper organizeMemberEntityMbgMapper;

    @Mock
    private OrganizeMemberDomainMapper organizeMemberDomainMapper;

    @InjectMocks
    private OrganizeMemberPersistenceAdapter adapter;

    @Test
    void save_persistsAndReturnsMappedDomain() {
        OrganizeMember member = OrganizeMember.create(OrganizeId.of(1L), MemberUserId.of(2L), OrganizeMemberRole.MEMBER, null);
        OrganizeMemberEntity entity = new OrganizeMemberEntity();
        OrganizeMember persisted = OrganizeMember.create(
                OrganizeId.of(1L),
                MemberUserId.of(2L),
                OrganizeMemberRole.MEMBER,
                LocalDateTime.of(2026, 1, 3, 12, 0));

        when(organizeMemberDomainMapper.toEntity(member)).thenReturn(entity);
        doAnswer(invocation -> {
            OrganizeMemberEntity argument = invocation.getArgument(0);
            argument.setId(11L);
            return 1;
        }).when(organizeMemberEntityMbgMapper).insertSelective(entity);
        when(organizeMemberDomainMapper.toDomain(entity)).thenReturn(persisted);

        OrganizeMember result = adapter.save(member);

        assertThat(result).isEqualTo(persisted);
        verify(organizeMemberEntityMbgMapper).insertSelective(entity);
    }

    @Test
    void existsByOrganizeIdAndUserId_usesCountResult() {
        when(organizeMemberEntityMbgMapper.countByCondition(org.mockito.ArgumentMatchers.any(OrganizeMemberEntityCondition.class)))
                .thenReturn(1L);

        boolean result = adapter.existsByOrganizeIdAndUserId(OrganizeId.of(1L), MemberUserId.of(2L));

        assertThat(result).isTrue();
        ArgumentCaptor<OrganizeMemberEntityCondition> captor = ArgumentCaptor.forClass(OrganizeMemberEntityCondition.class);
        verify(organizeMemberEntityMbgMapper).countByCondition(captor.capture());
        assertThat(captor.getValue().getOredCriteria()).isNotEmpty();
    }

    @Test
    void countOwnersByOrganizeId_usesOwnerRolePredicate() {
        when(organizeMemberEntityMbgMapper.countByCondition(org.mockito.ArgumentMatchers.any(OrganizeMemberEntityCondition.class)))
                .thenReturn(2L);

        assertThat(adapter.countOwnersByOrganizeId(1L)).isEqualTo(2L);
        verify(organizeMemberEntityMbgMapper).countByCondition(org.mockito.ArgumentMatchers.any(OrganizeMemberEntityCondition.class));
    }

    @Test
    void findAllByOrganizeId_mapsAllMembers() {
        OrganizeMemberEntity first = new OrganizeMemberEntity();
        first.setOrganizeId(1L);
        first.setUserId(2L);
        first.setRole("OWNER");
        OrganizeMemberEntity second = new OrganizeMemberEntity();
        second.setOrganizeId(1L);
        second.setUserId(3L);
        second.setRole("MEMBER");
        OrganizeMember firstDomain = mock(OrganizeMember.class);
        OrganizeMember secondDomain = mock(OrganizeMember.class);

        when(organizeMemberEntityMbgMapper.selectByCondition(org.mockito.ArgumentMatchers.any(OrganizeMemberEntityCondition.class)))
                .thenReturn(List.of(first, second));
        when(organizeMemberDomainMapper.toDomain(first)).thenReturn(firstDomain);
        when(organizeMemberDomainMapper.toDomain(second)).thenReturn(secondDomain);

        List<OrganizeMember> result = adapter.findAllByOrganizeId(OrganizeId.of(1L));

        assertThat(result).containsExactly(firstDomain, secondDomain);
    }
}
