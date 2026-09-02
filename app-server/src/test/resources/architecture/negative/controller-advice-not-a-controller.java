package io.jgitkins.server.common.presentation.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Both advice spellings contain a controller annotation name as a substring. The scanner matches with
// find(), so an unanchored pattern reports both of these as controllers.
@RestControllerAdvice
public class ControllerAdviceNotAController {
}

@ControllerAdvice
class AlsoNotAController {
}
