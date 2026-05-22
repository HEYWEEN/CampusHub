package com.campushub.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 默认敏感词检测：从 campushub.sensitive-words 配置 CSV 读，简单 contains。
 *
 * 设计取舍：
 *   - 朴素 contains O(n*m)，n=text 长度、m=词数；P0 阶段词数 ≤ 50 可接受
 *   - 真实生产应该用 AC 自动机 / DFA，但 admin 模块接管后由 admin 解决，本类不投资
 *   - 大小写敏感：中文场景下不需要 ignore case
 */
@Component
public class DefaultSensitiveWordChecker implements SensitiveWordChecker {

    private final List<String> words;

    public DefaultSensitiveWordChecker(
            @Value("${campushub.sensitive-words:傻逼,狗东西,卧槽,nmsl,fuck,shit}") String csv) {
        if (csv == null || csv.isBlank()) {
            this.words = Collections.emptyList();
        } else {
            this.words = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Override
    public String firstHit(String text) {
        if (text == null || text.isEmpty()) return null;
        for (String w : words) {
            if (text.contains(w)) return w;
        }
        return null;
    }
}
