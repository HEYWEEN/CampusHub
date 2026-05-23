package com.campushub.edu.service;

import com.campushub.edu.repository.ForbiddenWordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DefaultForbiddenWordMatcher implements ForbiddenWordMatcher {

    private final ForbiddenWordRepository wordRepo;

    public DefaultForbiddenWordMatcher(ForbiddenWordRepository wordRepo) {
        this.wordRepo = wordRepo;
    }

    @Override
    public Optional<String> match(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(text);
        List<String> words = wordRepo.findAllByOrderByWordAsc().stream()
                .map(w -> normalize(w.getWord()))
                .filter(w -> !w.isBlank())
                .toList();
        for (String word : words) {
            if (normalized.contains(word)) {
                return Optional.of(word);
            }
        }
        return Optional.empty();
    }

    /** 全角转半角 + 小写 + 去空白，简单谐音/emoji 兜底。 */
    static String normalize(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char ch : input.toCharArray()) {
            if (ch >= 0xFF01 && ch <= 0xFF5E) {
                sb.append((char) (ch - 0xFEE0));
            } else if (ch == 0x3000) {
                sb.append(' ');
            } else if (!Character.isWhitespace(ch) && !Character.isISOControl(ch)) {
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }
}
