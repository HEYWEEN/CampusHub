package com.campushub.edu.service;

import java.util.Optional;

public interface ForbiddenWordMatcher {

  Optional<String> match(String text);
}
