package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.contract.OrganizeCreateRequest;
import io.jgitkins.web.application.contract.OrganizeCreateResult;
import io.jgitkins.web.application.contract.OrganizeFetchResult;
import io.jgitkins.web.application.contract.OrganizeMemberSummary;
import java.util.List;

public interface OrganizePort {

	OrganizeFetchResult fetchOrganizes();

	OrganizeFetchResult fetchAccessibleOrganizes();

	OrganizeCreateResult createOrganize(OrganizeCreateRequest request);

	List<OrganizeMemberSummary> fetchOrganizeMembers(Long organizeId);
}
