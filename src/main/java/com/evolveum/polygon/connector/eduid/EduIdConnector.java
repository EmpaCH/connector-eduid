/*
 * Copyright (c) 2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.polygon.connector.eduid;

import com.evolveum.polygon.rest.AbstractRestConnector;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author oscar
 *
 */
@ConnectorClass(displayNameKey = "eduid.connector.display", configurationClass = EduIdConfiguration.class)
public class EduIdConnector extends AbstractRestConnector<EduIdConfiguration> implements PoolableConnector, TestOp, SchemaOp, CreateOp, DeleteOp, UpdateOp, SearchOp<EduIdFilter> {

    private static final Log LOG = Log.getLog(EduIdConnector.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String CONTENT_TYPE = "application/scim+json";
    protected static String AFFILIATION_OBJECT_CLASS = "affiliation"; // ObjectClass.ACCOUNT_NAME
    protected static String AFFILIATIONS = "Affiliations";
    private static String SERVICE_PROVIDER_CONFIG = "ServiceProviderConfig";

    /**
     * Returns the service base URI, normalised to always end with '/' so that
     * URI.resolve() of relative paths works correctly per RFC 3986.
     */
    private URI serviceUri() {
        String address = getConfiguration().getServiceAddress();
        if (!address.endsWith("/")) {
            address = address + "/";
        }
        return URI.create(address);
    }

    /**
     * Resolves a relative SCIM path against the normalised service base URI.
     * Each segment is percent-encoded individually so that special characters
     * like '@' in swissEduPersonUniqueIDs are handled correctly.
     */
    private String scimUrl(String... segments) {
        URI base = serviceUri();
        StringBuilder path = new StringBuilder();
        for (String segment : segments) {
            if (!path.isEmpty()) path.append('/');
            path.append(URLEncoder.encode(segment, StandardCharsets.UTF_8));
        }
        String relative = path.toString().replaceFirst("^/+", "");
        return base.resolve(relative).toString();
    }

    @Override
    public void test() {
        LOG.ok("test - reading ServiceProviderConfig");
        try {
            HttpGet request = new HttpGet(scimUrl(SERVICE_PROVIDER_CONFIG));
            JSONObject response = callRequest(request, true);
            LOG.ok("test - returning: {0}", response);
        } catch (IOException e) {
            throw new ConnectorIOException("Error when testing connection: " + e.getMessage(), e);
        }
    }

    @Override
    public void init(Configuration configuration) {
        super.init(configuration);
        LOG.ok("configuration: {0}", ((EduIdConfiguration) this.getConfiguration()).toString());
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public Schema schema() {
        SchemaBuilder schemaBuilder = new SchemaBuilder(EduIdConnector.class);
        buildAffiliationObjectClass(schemaBuilder);
        return schemaBuilder.build();
    }

    private void buildAffiliationObjectClass(SchemaBuilder schemaBuilder) {
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
        objClassBuilder.setType(AFFILIATION_OBJECT_CLASS);


        Collection<AttributeInfo> attributeInfoBuilders = Arrays.stream((EduIdScimAffiliation.class.getRecordComponents()))
                .filter(rc -> rc.getAccessor().getAnnotation(JsonProperty.class) != null)
                .map(rc -> {
                    LOG.info("processing record component: {0}", rc.getName());
                    JsonProperty jp = rc.getAccessor().getAnnotation(JsonProperty.class);

                    boolean required = jp.required();
                    boolean readOnly = jp.access() == JsonProperty.Access.READ_ONLY;
                    boolean multiValued = List.class.isAssignableFrom(rc.getType());
                    Class<?> elementType = multiValued
                            ? (Class<?>) ((ParameterizedType) rc.getGenericType()).getActualTypeArguments()[0]
                            : rc.getType();


                    AttributeInfoBuilder attr = new AttributeInfoBuilder(jp.value(), elementType);
                    attr.setRequired(required);
                    attr.setMultiValued(multiValued);
                    return attr.build();

                }).toList();
        objClassBuilder.addAllAttributeInfo(attributeInfoBuilders);
        LOG.info("built ObjectClassInfo for {0}: {1}", AFFILIATION_OBJECT_CLASS, objClassBuilder.build());
        schemaBuilder.defineObjectClass(objClassBuilder.build());
    }


    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {
        if (objectClass.is(AFFILIATION_OBJECT_CLASS)) {    // __ACCOUNT__
            return createOrUpdateAffiliation(null, attributes);
        } else {
            // not found
            throw new UnsupportedOperationException("Unsupported object class " + objectClass);
        }
    }



    protected JSONObject callRequest(HttpRequestBase request, boolean parseResult) throws IOException {
        LOG.ok("request URI: {0}", request.getURI());
        request.setHeader("Content-Type", CONTENT_TYPE);

        authHeader(request);

        CloseableHttpResponse response = null;
        response = execute(request);
        LOG.ok("response: {0}", response);
        processEduIdResponseErrors(response, request);

        if (!parseResult) {
            closeResponse(response);
            return null;
        }
        String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        LOG.ok("response body: {0}", result);
        closeResponse(response);
        return new JSONObject(result);
    }

    private void authHeader(HttpRequestBase request) {
        // to prevent several calls http://stackoverflow.com/questions/20914311/httpclientbuilder-basic-auth
        // auth header
        final StringBuilder sb = new StringBuilder();
        if (getConfiguration().getPassword() != null) {
            getConfiguration().getPassword().access(new GuardedString.Accessor() {
                @Override
                public void access(char[] chars) {
                    sb.append(new String(chars));
                }
            });
        } else {
            return;
        }
        byte[] credentials = Base64.encodeBase64((getConfiguration().getUsername() + ":" + sb.toString()).getBytes(StandardCharsets.UTF_8));
        request.setHeader("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));
    }


    /**
     * Parsed representation of a SCIM error response body.
     */
    private static class ScimError {
        final String detail;
        final String scimType;

        ScimError(String detail, String scimType) {
            this.detail = detail;
            this.scimType = scimType;
        }
    }

    private ScimError readScimError(CloseableHttpResponse response) {
        try {
            String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            LOG.ok("error body: {0}", result);
            JSONObject jo = new JSONObject(result);
            return new ScimError(
                    jo.optString("detail", "(no detail)"),
                    jo.optString("scimType", "(no scimType)")
            );
        } catch (IOException | JSONException e) {
            LOG.ok("could not parse SCIM error body: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Handle HTTP responses by matching on method + endpoint + status code,
     * mirroring the semantics defined in the eduID OpenAPI spec.
     * <p>
     * 404 is NOT a generic HTTP error — its meaning depends on context:
     * GET    /Affiliations/{id} 404 → affiliation not found / former → return null to caller
     * DELETE /Affiliations/{id} 404 → already gone, idempotent → silently ignore
     * PUT    /Affiliations/{id} 404 → swissEduID invalid → InvalidAttributeValueException
     * POST   /Affiliations      404 → swissEduID invalid → InvalidAttributeValueException
     * <p>
     * 409 on POST → AlreadyExistsException
     * 400 on any  → InvalidAttributeValueException
     */
    /**
     * Returns true if the caller should proceed to read the response body.
     * Returns false if the response has been fully handled (404, 204 etc.)
     * and the caller should return null.
     * Throws on error conditions.
     */
    private boolean processEduIdResponseErrors(CloseableHttpResponse response, HttpRequestBase request) {
        int statusCode = response.getStatusLine().getStatusCode();
        String uri = request.getURI().toString();
        LOG.ok("processEduIdResponseErrors op: {0}, URI: {1}, status: {2}",
                EduIdScimOperation.from(request), uri, statusCode);

        switch (EduIdScimOperation.from(request)) {
            case EduIdScimOperation.Get get -> {
                if (statusCode == 200) return true;
                if (statusCode == 404) {
                    LOG.ok("get operation, affiliation not found or former, returning null to caller");
                    EntityUtils.consumeQuietly(response.getEntity());
                    closeResponse(response);
                    return false;
                }
            }
            case EduIdScimOperation.GetAll getAll -> {
                if (statusCode == 200) return true;
            }
            case EduIdScimOperation.Post post -> {
                if (statusCode == 201) return true;
                if (statusCode == 404) {
                    ScimError err = readScimError(response);
                    closeResponse(response);
                    throw new InvalidAttributeValueException(
                            "swissEduID does not match any existing edu-ID user" +
                                    (err != null ? ": " + err.detail : "") + " [" + uri + "]");
                }
                if (statusCode == 409) {
                    ScimError err = readScimError(response);
                    closeResponse(response);
                    throw err != null && "uniqueness".equals(err.scimType)
                            ? new AlreadyExistsException("Affiliation already exists: " + err.detail)
                            : new ConnectorIOException("Conflict: " + uri);
                }
            }
            case EduIdScimOperation.Put put -> {
                if (statusCode == 200) return true;
                if (statusCode == 404) {
                    ScimError err = readScimError(response);
                    closeResponse(response);
                    throw new InvalidAttributeValueException(
                            "swissEduID does not match any existing edu-ID user" +
                                    (err != null ? ": " + err.detail : "") + " [" + uri + "]");
                }
            }
            case EduIdScimOperation.Delete delete -> {
                if (statusCode == 404 || statusCode == 204) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    closeResponse(response);
                    return false;
                }
            }
        }

        // fallthrough — unexpected status, delegate to base class
        EntityUtils.consumeQuietly(response.getEntity());
        super.processResponseErrors(response);
        return false;
    }

    private CloseableHttpResponse executeScimRequest(HttpRequestBase request) throws IOException {
        request.setHeader("Content-Type", CONTENT_TYPE);
        request.setHeader("Accept", CONTENT_TYPE);
        authHeader(request);
        LOG.ok("request URI: {0}, method: {1}", request.getURI(), request.getMethod());
        CloseableHttpResponse response = execute(request);
        LOG.ok("response status: {0}", response.getStatusLine().getStatusCode());
        boolean hasBody = processEduIdResponseErrors(response, request);
        return hasBody ? response : null; // null signals "handled, no body"
    }


    protected <T> T get(String url, JavaType responseType) throws IOException {
        CloseableHttpResponse response = executeScimRequest(new HttpGet(url));
        if (response == null) return null;
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        closeResponse(response);
        LOG.info("response body: {0}", body);
        return mapper.readValue(body, responseType);
    }

    protected <T> T get(String url, Class<T> responseType) throws IOException {
        return get(url, mapper.constructType(responseType));
    }

    protected <T, R> R post(String url, T body, Class<R> responseType) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(body)));
        CloseableHttpResponse response = executeScimRequest(request);
        String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        closeResponse(response);
        return mapper.readValue(result, responseType);
    }

    protected <T, R> R put(String url, T body, Class<R> responseType) throws IOException {
        HttpPut request = new HttpPut(url);
        request.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(body)));
        CloseableHttpResponse response = executeScimRequest(request);
        String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        closeResponse(response);
        return mapper.readValue(result, responseType);
    }

    protected void delete(String url) throws IOException {
        closeResponse(executeScimRequest(new HttpDelete(url)));
    }


    private Uid createOrUpdateAffiliation(Uid uid, Set<Attribute> attributes) {
        LOG.ok("createOrUpdateAffiliation, Uid: {0}, attributes: {1}", uid, attributes);
        if (attributes == null || attributes.isEmpty()) {
            LOG.ok("request ignored, empty attributes");
            return uid;
        }

        try {
            boolean create = uid == null;

            if (create) {
                EduIdScimAffiliation request = EduIdScimAffiliation.from(attributes);
                LOG.ok("creating affiliation: {0}", request);
                EduIdScimAffiliation created = post(scimUrl(AFFILIATIONS), request, EduIdScimAffiliation.class);
                LOG.info("affiliation created, UID: {0}", created.swissEduPersonUniqueID());
                return new Uid(created.swissEduPersonUniqueID());
            } else {
                EduIdScimAffiliation existing = get(scimUrl(AFFILIATIONS, uid.getUidValue()), EduIdScimAffiliation.class);
                if (existing == null) {
                    throw new UnknownUidException("Affiliation with ID " + uid.getUidValue() + " does not exist");
                }
                EduIdScimAffiliation request = existing.merge(attributes);
                LOG.ok("updating affiliation: {0}", request);
                EduIdScimAffiliation updated = put(scimUrl(AFFILIATIONS, uid.getUidValue()), request, EduIdScimAffiliation.class);
                LOG.info("affiliation updated, UID: {0}", updated.swissEduPersonUniqueID());
                return new Uid(updated.swissEduPersonUniqueID());
            }
        } catch (IOException e) {
            throw new ConnectorIOException(e.getMessage(), e);
        }
    }


    @Override
    public void checkAlive() {
        test();
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {
        try {
            if (objectClass.is(AFFILIATION_OBJECT_CLASS)) {
                LOG.ok("delete affiliation, Uid: {0}", uid);
                delete(scimUrl(AFFILIATIONS, uid.getUidValue()));
            } else {
                // not found
                throw new UnsupportedOperationException("Unsupported object class " + objectClass);
            }
        } catch (IOException e) {
            throw new ConnectorIOException(e.getMessage(), e);
        }
    }

    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions operationOptions) {
        if (objectClass.is(AFFILIATION_OBJECT_CLASS)) {
            return createOrUpdateAffiliation(uid, attributes);
        } else {
            // not found
            throw new UnsupportedOperationException("Unsupported object class " + objectClass);
        }
    }


    @Override
    public FilterTranslator<EduIdFilter> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
        return new EduIdFilterTranslator();
    }

    @Override
    public void executeQuery(ObjectClass objectClass, EduIdFilter query, ResultsHandler handler, OperationOptions options) {
        try {
            LOG.info("executeQuery on {0}, query: {1}, options: {2}", objectClass, query, options);
            if (objectClass.is(AFFILIATION_OBJECT_CLASS)) {
                //find by Uid (user Primary Key)
                if (query != null && query.byUid != null) {
                    LOG.info("executeQuery, searching affiliation by UID: {0}", query.byUid);
                    EduIdScimAffiliation affiliation = get(scimUrl(AFFILIATIONS, query.byUid), EduIdScimAffiliation.class);
                    LOG.info("executeQuery, affiliation found: {0}", affiliation);
                    if (affiliation == null) {
                        throw new UnknownUidException("Affiliation with ID " + query.byUid + " does not exist");
                    }
                    ConnectorObject connectorObject = affiliation.toConnectorObject();
                    handler.handle(connectorObject);
                } else if (query == null) {

                    JavaType listType = mapper.getTypeFactory()
                            .constructParametricType(ScimListResponse.class, EduIdScimAffiliation.class);
                    ScimListResponse<EduIdScimAffiliation> response = get(scimUrl(AFFILIATIONS), listType);
                    LOG.info("executeQuery, total affiliations found: {0}", response);
                    for (EduIdScimAffiliation affiliation : response.getResources()) {
                        ConnectorObject connectorObject = affiliation.toConnectorObject();
                        handler.handle(connectorObject);
                    }

                }

            } else {
                // not found
                throw new UnsupportedOperationException("Unsupported object class " + objectClass);
            }
        } catch (IOException e) {
            LOG.info("executeQuery error: {0}", e.getMessage());
            throw new ConnectorIOException(e.getMessage(), e);
        }
    }


}