package com.evolveum.polygon.connector.eduid;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;

import java.util.List;
import java.util.Set;

/**
 * SCIM Affiliation request payload — contains only fields that are writable
 * per the SWITCH edu-ID OpenAPI spec. Read-only fields such as
 * eduPersonScopedAffiliation, swissEduPersonHomeOrganization (server-derived)
 * are intentionally omitted so they cannot be accidentally sent.
 *
 * Required fields are validated in the static factory {@link #from(Set)}.
 * Optional fields default to null and are excluded from serialization via
 * {@link JsonInclude.Include#NON_NULL}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EduIdScimAffiliationRequest(

        // --- Always set to the standard schema URN ---
        @JsonProperty(value = "schemas", required = true)
        List<String> schemas,

        // --- Required identity fields ---
        @JsonProperty(value = "id", required = true)
        String id,

        @JsonProperty(value = "externalId", required = true)
        String externalId,

        @JsonProperty(value = "swissEduPersonUniqueID", required = true)
        String swissEduPersonUniqueID,

        @JsonProperty(value = "swissEduID", required = true)
        String swissEduID,

        // --- Required name fields ---
        @JsonProperty(value = "givenName", required = true)
        String givenName,

        @JsonProperty(value = "surname", required = true)
        String surname,

        // --- Required affiliation fields ---
        @JsonProperty(value = "swissEduIDAffiliationStatus", required = true)
        String swissEduIDAffiliationStatus,

        @JsonProperty(value = "swissEduIDAffiliationPeriodBegin", required = true)
        String swissEduIDAffiliationPeriodBegin,

        @JsonProperty(value = "eduPersonAffiliation", required = true)
        List<String> eduPersonAffiliation,

        @JsonProperty(value = "email", required = true)
        List<String> email,

        // --- Optional single-value fields ---
        @JsonProperty(value = "displayName", required = false)
        String displayName,

        @JsonProperty(value = "commonName", required = false)
        List<String> commonName,

        @JsonProperty(value = "eduPersonUniqueId", required = false)
        String eduPersonUniqueId,

        @JsonProperty("eduPersonPrincipalName")
        String eduPersonPrincipalName,

        @JsonProperty("eduPersonPrimaryAffiliation")
        String eduPersonPrimaryAffiliation,

        @JsonProperty("eduPersonPrimaryOrgUnitDN")
        String eduPersonPrimaryOrgUnitDN,

        @JsonProperty("eduPersonOrgDN")
        String eduPersonOrgDN,

        @JsonProperty("schacHomeOrganization")
        String schacHomeOrganization,

        @JsonProperty("preferredLanguage")
        String preferredLanguage,

        @JsonProperty("employeeNumber")
        String employeeNumber,

        @JsonProperty("uid")
        String uid,

        // NOTE: String enum "0","1","2","9" per OpenAPI spec — NOT an integer
        @JsonProperty("swissEduPersonGender")
        String swissEduPersonGender,

        @JsonProperty("swissEduPersonDateOfBirth")
        String swissEduPersonDateOfBirth,

        @JsonProperty("swissEduPersonMatriculationNumber")
        String swissEduPersonMatriculationNumber,

        @JsonProperty("fschImapPW")
        String fschImapPW,

        // --- Optional multi-value string fields ---
        @JsonProperty("eduPersonAssurance")
        List<String> eduPersonAssurance,

        @JsonProperty("eduPersonEntitlement")
        List<String> eduPersonEntitlement,

        @JsonProperty("eduPersonNickname")
        List<String> eduPersonNickname,

        @JsonProperty("eduPersonOrgUnitDN")
        List<String> eduPersonOrgUnitDN,

        @JsonProperty("eduPersonTargetedID")
        List<String> eduPersonTargetedID,

        @JsonProperty("schacHomeOrganizationType")
        List<String> schacHomeOrganizationType,

        @JsonProperty("swissEduPersonCardUID")
        List<String> swissEduPersonCardUID,

        @JsonProperty("swissEduPersonStudyLevel")
        List<String> swissEduPersonStudyLevel,

        @JsonProperty("swissLibraryPersonAffiliation")
        List<String> swissLibraryPersonAffiliation,

        @JsonProperty("swissLibraryPersonResidence")
        List<String> swissLibraryPersonResidence,

        @JsonProperty("telephoneNumber")
        List<String> telephoneNumber,

        @JsonProperty("mobile")
        List<String> mobile,

        @JsonProperty("homePhone")
        List<String> homePhone,

        @JsonProperty("postalAddress")
        List<String> postalAddress,

        @JsonProperty("homePostalAddress")
        List<String> homePostalAddress,

        @JsonProperty("isMemberOf")
        List<String> isMemberOf,

        @JsonProperty("ou")
        List<String> ou,

        // --- Optional multi-value integer fields ---
        @JsonProperty("swissEduPersonStaffCategory")
        List<Integer> swissEduPersonStaffCategory,

        @JsonProperty("swissEduPersonStudyBranch1")
        List<Integer> swissEduPersonStudyBranch1,

        @JsonProperty("swissEduPersonStudyBranch2")
        List<Integer> swissEduPersonStudyBranch2,

        @JsonProperty("swissEduPersonStudyBranch3")
        List<Integer> swissEduPersonStudyBranch3

) {
    /** Standard schema URN for all affiliation requests. */
    private static final String SCHEMAS_VALUE = "urn:mace:switch.ch:eduid:scim:1.0:affiliation";

    /**
     * Constructs a {@link EduIdScimAffiliationRequest} from a ConnId attribute set,
     * validating all required fields and mapping optional ones where present.
     *
     * @throws InvalidAttributeValueException if any required attribute is missing or blank
     */
    public static EduIdScimAffiliationRequest from(Set<Attribute> attributes) {
        return new EduIdScimAffiliationRequest(
                // schemas — always the standard URN
                List.of(SCHEMAS_VALUE),

                // required single-value
                required(attributes, "id"),
                required(attributes, "externalId"),
                required(attributes, "swissEduPersonUniqueID"),
                required(attributes, "swissEduID"),
                required(attributes, "givenName"),
                required(attributes, "surname"),
                required(attributes, "swissEduIDAffiliationStatus"),
                required(attributes, "swissEduIDAffiliationPeriodBegin"),

                // required multi-value
                requiredMulti(attributes, "eduPersonAffiliation"),
                requiredMulti(attributes, "email"),

                // optional single-value
                optional(attributes, "displayName"),
                optionalMulti(attributes, "commonName"),
                optional(attributes, "eduPersonUniqueId"),
                optional(attributes, "eduPersonPrincipalName"),
                optional(attributes, "eduPersonPrimaryAffiliation"),
                optional(attributes, "eduPersonPrimaryOrgUnitDN"),
                optional(attributes, "eduPersonOrgDN"),
                optional(attributes, "schacHomeOrganization"),
                optional(attributes, "preferredLanguage"),
                optional(attributes, "employeeNumber"),
                optional(attributes, "uid"),
                optional(attributes, "swissEduPersonGender"),
                optional(attributes, "swissEduPersonDateOfBirth"),
                optional(attributes, "swissEduPersonMatriculationNumber"),
                optional(attributes, "fschImapPW"),

                // optional multi-value string
                optionalMulti(attributes, "eduPersonAssurance"),
                optionalMulti(attributes, "eduPersonEntitlement"),
                optionalMulti(attributes, "eduPersonNickname"),
                optionalMulti(attributes, "eduPersonOrgUnitDN"),
                optionalMulti(attributes, "eduPersonTargetedID"),
                optionalMulti(attributes, "schacHomeOrganizationType"),
                optionalMulti(attributes, "swissEduPersonCardUID"),
                optionalMulti(attributes, "swissEduPersonStudyLevel"),
                optionalMulti(attributes, "swissLibraryPersonAffiliation"),
                optionalMulti(attributes, "swissLibraryPersonResidence"),
                optionalMulti(attributes, "telephoneNumber"),
                optionalMulti(attributes, "mobile"),
                optionalMulti(attributes, "homePhone"),
                optionalMulti(attributes, "postalAddress"),
                optionalMulti(attributes, "homePostalAddress"),
                optionalMulti(attributes, "isMemberOf"),
                optionalMulti(attributes, "ou"),

                // optional multi-value integer
                optionalIntMulti(attributes, "swissEduPersonStaffCategory"),
                optionalIntMulti(attributes, "swissEduPersonStudyBranch1"),
                optionalIntMulti(attributes, "swissEduPersonStudyBranch2"),
                optionalIntMulti(attributes, "swissEduPersonStudyBranch3")
        );
    }

    // --- Private extraction helpers ---

    private static String required(Set<Attribute> attributes, String name) {
        return attributes.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .map(a -> {
                    if (a.getValue() == null || a.getValue().isEmpty() || a.getValue().get(0) == null) {
                        throw new InvalidAttributeValueException("Required attribute is blank: " + name);
                    }
                    return (String) a.getValue().get(0);
                })
                .orElseThrow(() -> new InvalidAttributeValueException("Missing required attribute: " + name));
    }

    private static List<String> requiredMulti(Set<Attribute> attributes, String name) {
        return attributes.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .map(a -> {
                    if (a.getValue() == null || a.getValue().isEmpty()) {
                        throw new InvalidAttributeValueException("Required multi-value attribute is empty: " + name);
                    }
                    return a.getValue().stream().map(Object::toString).toList();
                })
                .orElseThrow(() -> new InvalidAttributeValueException("Missing required attribute: " + name));
    }

    private static String optional(Set<Attribute> attributes, String name) {
        return attributes.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .filter(a -> a.getValue() != null && !a.getValue().isEmpty() && a.getValue().get(0) != null)
                .map(a -> (String) a.getValue().get(0))
                .orElse(null);
    }

    private static List<String> optionalMulti(Set<Attribute> attributes, String name) {
        return attributes.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .filter(a -> a.getValue() != null && !a.getValue().isEmpty())
                .map(a -> a.getValue().stream().map(Object::toString).toList())
                .orElse(null);
    }

    private static List<Integer> optionalIntMulti(Set<Attribute> attributes, String name) {
        return attributes.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .filter(a -> a.getValue() != null && !a.getValue().isEmpty())
                .map(a -> a.getValue().stream()
                        .map(v -> Integer.parseInt(v.toString()))
                        .toList())
                .orElse(null);
    }
}