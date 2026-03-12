package com.evolveum.polygon.connector.eduid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.evolveum.polygon.connector.eduid.EduIdConnector.AFFILIATION_OBJECT_CLASS;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EduIdScimAffiliation(

        @JsonProperty(value = "schemas")
        List<String> schemas,

        @JsonProperty(value = "id", required = true)
        String id,

        @JsonProperty(value = "externalId", required = true)
        String externalId,

        @JsonProperty(value = "swissEduPersonUniqueID", required = true)
        String swissEduPersonUniqueID,

        @JsonProperty(value = "swissEduID", required = true)
        String swissEduID,

        @JsonProperty(value = "givenName", required = true)
        String givenName,

        @JsonProperty(value = "surname", required = true)
        String surname,

        @JsonProperty(value = "swissEduIDAffiliationStatus", required = true)
        String swissEduIDAffiliationStatus,

        @JsonProperty(value = "swissEduIDAffiliationPeriodBegin", required = true)
        String swissEduIDAffiliationPeriodBegin,

        @JsonProperty(value = "eduPersonAffiliation", required = true)
        List<String> eduPersonAffiliation,

        @JsonProperty(value = "email", required = true)
        List<String> email,

        // read-only — derived by server, never sent in requests
        @JsonProperty(value = "eduPersonScopedAffiliation", access = JsonProperty.Access.READ_ONLY)
        List<String> eduPersonScopedAffiliation,

        @JsonProperty(value = "swissEduPersonHomeOrganization", access = JsonProperty.Access.READ_ONLY)
        String swissEduPersonHomeOrganization,

        @JsonProperty(value = "swissEduPersonHomeOrganizationType", access = JsonProperty.Access.READ_ONLY)
        String swissEduPersonHomeOrganizationType,

        // optional writable single-value
        @JsonProperty("displayName")
        String displayName,

        @JsonProperty("commonName")
        List<String> commonName,

        @JsonProperty("eduPersonUniqueId")
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

        // String enum "0","1","2","9" per OpenAPI spec — NOT an integer
        @JsonProperty("swissEduPersonGender")
        String swissEduPersonGender,

        @JsonProperty("swissEduPersonDateOfBirth")
        String swissEduPersonDateOfBirth,

        @JsonProperty("swissEduPersonMatriculationNumber")
        String swissEduPersonMatriculationNumber,

        @JsonProperty("fschImapPW")
        String fschImapPW,

        // optional writable multi-value string
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


        @JsonProperty("eduPersonAssurance")
        List<String> eduPersonAssurance,

        @JsonProperty("swissEduIDAssuranceLevel")
        List<String> swissEduIDAssuranceLevel,

        @JsonProperty("eduPersonEntitlement")
        List<String> eduPersonEntitlement,

        @JsonProperty("eduPersonNickname")
        List<String> eduPersonNickname,

        @JsonProperty("eduPersonOrgUnitDN")
        List<String> eduPersonOrgUnitDN,

        @JsonProperty("eduPersonTargetedID")
        List<String> eduPersonTargetedID,

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

        // optional writable multi-value integer
        @JsonProperty("swissEduPersonStaffCategory")
        List<Integer> swissEduPersonStaffCategory,

        @JsonProperty("swissEduPersonStudyBranch1")
        List<Integer> swissEduPersonStudyBranch1,

        @JsonProperty("swissEduPersonStudyBranch2")
        List<Integer> swissEduPersonStudyBranch2,

        @JsonProperty("swissEduPersonStudyBranch3")
        List<Integer> swissEduPersonStudyBranch3,

        @JsonProperty("eduPersonOrcid")
        List<String> eduPersonOrcid,

        @JsonProperty("userPrincipalName")
        String userPrincipalName



) {
    private static final String SCHEMAS_VALUE = "urn:mace:switch.ch:eduid:scim:1.0:affiliation";
    private static final RecordComponent[] COMPONENTS = EduIdScimAffiliation.class.getRecordComponents();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Static factory for create operations.
     * Required fields are validated eagerly; optional fields are null if absent
     * (omitted from serialization by @JsonInclude(NON_NULL)).
     * READ_ONLY fields are always null — never sent to the server.
     */
    public static EduIdScimAffiliation from(Set<Attribute> attributes) {
        return construct(attributes, null);
    }

    /**
     * Merges incoming ConnId attributes over this existing server record.
     * For each writable field: incoming value wins if present, existing value otherwise.
     * READ_ONLY and schemas fields are always preserved from the existing record.
     * Driven entirely by @JsonProperty annotations — no field list to maintain.
     */
    public EduIdScimAffiliation merge(Set<Attribute> attributes) {
        return construct(attributes, this);
    }

    /**
     * Returns all writable, non-schemas record components for ConnId schema generation.
     * Excludes READ_ONLY fields — midPoint should not expose them as settable attributes.
     */
    public static List<RecordComponent> writableComponents() {
        return Arrays.stream(COMPONENTS)
                .filter(rc -> {
                    JsonProperty jp = rc.getAccessor().getAnnotation(JsonProperty.class);
                    return jp != null
                            && !rc.getName().equals("schemas")
                            && jp.access() != JsonProperty.Access.READ_ONLY;
                })
                .toList();
    }

    /**
     * Converts this record to a ConnId object.
     */
    public ConnectorObject toConnectorObject() {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        ObjectClass objectClass = new ObjectClass(AFFILIATION_OBJECT_CLASS);
        builder.setObjectClass(objectClass);
        String localUid = swissEduPersonUniqueID;
        builder.setUid(new Uid(localUid));
        builder.setName(localUid);
        for (RecordComponent rc : COMPONENTS) {
            JsonProperty jp = rc.getAccessor().getAnnotation(JsonProperty.class);
            if (jp == null || rc.getName().equals("schemas")) continue;

            Object value = currentValue(rc, this);
            if (value == null) continue;

            String fieldName = jp.value();

            if (value instanceof List<?> list) {
                if (list.isEmpty()) continue;
                builder.addAttribute(AttributeBuilder.build(fieldName, list));
            } else {
                builder.addAttribute(AttributeBuilder.build(fieldName, value));
            }
        }

        return builder.build();
    }
    // -------------------------------------------------------------------------
    // Core construction — drives both from() and merge()
    // -------------------------------------------------------------------------

    /**
     * Constructs an EduIdScimAffiliation by iterating record components and resolving
     * each field value from annotations + attribute set + optional existing record.
     * <p>
     * Rules per component:
     * - schemas:    always the standard URN (create) or preserved from existing (merge)
     * - READ_ONLY:  always null (create) or preserved from existing (merge)
     * - required:   extracted from attributes, throws if missing/blank (create only)
     * - optional:   extracted from attributes if present, falls back to existing (merge) or null (create)
     */
    private static EduIdScimAffiliation construct(Set<Attribute> attributes, EduIdScimAffiliation existing) {
        Object[] values = new Object[COMPONENTS.length];

        for (int i = 0; i < COMPONENTS.length; i++) {
            RecordComponent rc = COMPONENTS[i];
            JsonProperty jp = rc.getAccessor().getAnnotation(JsonProperty.class);

            // schemas — fixed URN on create, preserved on merge
            if (rc.getName().equals("schemas")) {
                values[i] = existing != null ? existing.schemas() : List.of(SCHEMAS_VALUE);
                continue;
            }

            // no annotation — shouldn't happen, preserve existing or null
            if (jp == null) {
                values[i] = existing != null ? currentValue(rc, existing) : null;
                continue;
            }

            // READ_ONLY — never sent to server; preserve from existing on merge
            if (jp.access() == JsonProperty.Access.READ_ONLY) {
                values[i] = existing != null ? currentValue(rc, existing) : null;
                continue;
            }

            String fieldName = jp.value();
            boolean isList = List.class.isAssignableFrom(rc.getType());
            Object existingValue = existing != null ? currentValue(rc, existing) : null;

            if (jp.required() && existing == null) {
                // create — required fields must be present in the attribute set
                values[i] = isList
                        ? requiredMulti(attributes, fieldName)
                        : required(attributes, fieldName);
            } else {
                // merge — incoming wins, fall back to existing; or null on create for optional fields
                Object incoming = isList
                        ? resolveList(rc, attributes, fieldName)
                        : optional(attributes, fieldName);
                values[i] = incoming != null ? incoming : existingValue;
            }
        }

        try {
            return (EduIdScimAffiliation) EduIdScimAffiliation.class
                    .getDeclaredConstructors()[0]
                    .newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to construct EduIdScimAffiliation", e);
        }
    }

    /**
     * Resolves a List-typed field from attributes, dispatching on element type.
     * Integer lists use optionalIntMulti; all others use optionalMulti (String).
     */
    private static List<?> resolveList(RecordComponent rc, Set<Attribute> attributes, String fieldName) {
        Class<?> elementType = (Class<?>)
                ((ParameterizedType) rc.getGenericType()).getActualTypeArguments()[0];
        return elementType == Integer.class
                ? optionalIntMulti(attributes, fieldName)
                : optionalMulti(attributes, fieldName);
    }

    /**
     * Reads the current value of a record component from an existing instance via its accessor.
     */
    private static Object currentValue(RecordComponent rc, EduIdScimAffiliation instance) {
        try {
            return rc.getAccessor().invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read record component: " + rc.getName(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Attribute extraction helpers
    // -------------------------------------------------------------------------

    private static String required(Set<Attribute> attributes, String name) {
        return attributes.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .map(a -> {
                    if (a.getValue() == null || a.getValue().isEmpty() || a.getValue().get(0) == null)
                        throw new InvalidAttributeValueException("Required attribute is blank: " + name);
                    return (String) a.getValue().get(0);
                })
                .orElseThrow(() -> new InvalidAttributeValueException("Missing required attribute: " + name));
    }

    private static List<String> requiredMulti(Set<Attribute> attributes, String name) {
        return attributes.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .map(a -> {
                    if (a.getValue() == null || a.getValue().isEmpty())
                        throw new InvalidAttributeValueException("Required attribute is empty: " + name);
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