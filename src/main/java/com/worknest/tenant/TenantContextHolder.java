package com.worknest.tenant;

import java.util.Optional;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantSessionContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantSessionContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantSessionContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
