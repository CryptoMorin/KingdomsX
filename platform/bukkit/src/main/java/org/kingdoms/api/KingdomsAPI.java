package org.kingdoms.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Experimental
public interface KingdomsAPI {
    static KingdomsAPI getApi() {
        return Objects.requireNonNull(KingdomsDefaultAPIContainer.API, "Kingdoms API not ready yet");
    }

    KingdomsActionProcessor getActionProcessor();
}
