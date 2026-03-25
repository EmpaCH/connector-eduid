package com.evolveum.polygon.connector.eduid;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SwissEduIDAffiliationSecurityPolicy(
        @Flatten
        @JsonProperty("mfaPolicy") Policy mfaPolicy
) {
    public record Policy(
            @JsonProperty("mode") String mode,
            @JsonProperty("maxDeviceTrustDuration") String maxDeviceTrustDuration,
            @JsonProperty("allowedSecondFactorTypes") List<String> allowedSecondFactorTypes

    ) {}
}