package io.jgitkins.server.repository.adapter.in.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

// Proves the category fires on both spellings, and on the annotation-with-arguments form. Without
// this the disk set could be empty for a reason nothing else would surface.
@RestController("namedBean")
public class ControllerDeclaration {
}

@Controller
class WebControllerDeclaration {
}
