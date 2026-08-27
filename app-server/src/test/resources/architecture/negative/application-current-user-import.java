package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;

public class ApplicationCurrentUserImport {
    // The import alone is the violation; a field would make it two matches for
    // one (fixture, category) pair, and the pair is the assertion unit.
}
