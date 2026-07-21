package com.nexusadmin.api.auth;

import com.nexusadmin.api.auth.PermissionResolver.PermissionCheck;
import com.nexusadmin.api.auth.PermissionResolver.PermissionDecision;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.util.HttpAuthUtils;
import com.nexusadmin.core.extension.ExtensionConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionAccessTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentUserHas_shouldUseResolverAndCacheWithinRequest() {
        ExtensionConsumer<PermissionResolver> consumer = mock(ExtensionConsumer.class);
        PermissionResolver resolver = mock(PermissionResolver.class);
        when(consumer.get()).thenReturn(Optional.of(resolver));
        when(resolver.decide(any(PermissionCheck.class), any(InvocationContext.class)))
                .thenReturn(new PermissionDecision(true, "允许", Set.of("config.manage")));
        MockHttpServletRequest request = authenticatedRequest();
        PermissionAccess access = new PermissionAccess(consumer);

        assertTrue(access.currentUserHas("config.manage"));
        assertTrue(access.currentUserHas("config.manage"));

        verify(resolver, times(1)).decide(any(PermissionCheck.class), any(InvocationContext.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentUserHas_shouldFailClosedWithoutResolverOrRequest() {
        ExtensionConsumer<PermissionResolver> consumer = mock(ExtensionConsumer.class);
        when(consumer.get()).thenReturn(Optional.empty());
        PermissionAccess access = new PermissionAccess(consumer);

        assertFalse(access.currentUserHas("config.view"));

        authenticatedRequest();
        assertFalse(access.currentUserHas("config.view"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void check_shouldSplitNestedPermissionAtLastDot() {
        ExtensionConsumer<PermissionResolver> consumer = mock(ExtensionConsumer.class);
        PermissionResolver resolver = mock(PermissionResolver.class);
        when(consumer.get()).thenReturn(Optional.of(resolver));
        when(resolver.decide(any(PermissionCheck.class), any(InvocationContext.class)))
                .thenReturn(new PermissionDecision(true, "允许", Set.of("config.document.*")));
        MockHttpServletRequest request = authenticatedRequest();

        assertTrue(new PermissionAccess(consumer).check(request, "config.document.view").allowed());
        verify(resolver).decide(argThat(check ->
                        "config.document".equals(check.resource()) && "view".equals(check.action())),
                any(InvocationContext.class));
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        HttpAuthUtils.setSessionUser(request, "admin");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }
}
