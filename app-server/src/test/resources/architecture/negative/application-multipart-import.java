package io.jgitkins.server.repository.application.service;

import org.springframework.web.multipart.MultipartFile;

public class ApplicationMultipartImport {
    // The import alone is the violation; a field would make it two matches for
    // one (fixture, category) pair, and the pair is the assertion unit.
}
