package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.contract.ExploreSummary;

public interface ExploreFacadeUseCase {

    ExploreSummary getExploreSummary(String type);

}
