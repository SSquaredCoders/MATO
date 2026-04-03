package com.lshzzz.mato.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class GoogleOAuthEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("GOOGLE_CLIENT_ID"))
            && StringUtils.hasText(context.getEnvironment().getProperty("GOOGLE_CLIENT_SECRET"));
    }
}
