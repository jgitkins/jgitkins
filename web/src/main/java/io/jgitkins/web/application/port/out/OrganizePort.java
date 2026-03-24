package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.dto.OrganizeCreateRequest;
import io.jgitkins.web.application.dto.OrganizeCreateResult;
import io.jgitkins.web.application.dto.OrganizeFetchResult;
import io.jgitkins.web.application.dto.OrganizeMemberSummary;
import java.util.List;

public interface OrganizePort {

	OrganizeFetchResult fetchOrganizes();

	OrganizeFetchResult fetchAccessibleOrganizes();

	OrganizeCreateResult createOrganize(OrganizeCreateRequest request);

	List<OrganizeMemberSummary> fetchOrganizeMembers(Long organizeId);
}
