package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.dto.ExploreSummary;

public interface ExploreFacadeUseCase {

    ExploreSummary getExploreSummary(String type);

}
