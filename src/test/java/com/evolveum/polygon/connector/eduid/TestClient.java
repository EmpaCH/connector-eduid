/**
 * Copyright (c) 2019 Evolveum
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.polygon.connector.eduid;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author oscar
 */
public class TestClient {

    private static final Log LOG = Log.getLog(TestClient.class);

    private static EduIdConnector conn;

    private static String DOMAIN = "";
    private static String ID = "";

    ObjectClass affiliationObjectClass = new ObjectClass(EduIdConnector.AFFILIATION_OBJECT_CLASS);
    private  static  final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void setUp() throws Exception {
        String fileName = "test.properties";

        final Properties properties = new Properties();
        InputStream inputStream = TestClient.class.getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IOException("Unable to find configuration for test: " + fileName);
        }
        properties.load(inputStream);

        EduIdConfiguration conf = new EduIdConfiguration();
        conf.setUsername(properties.getProperty("username"));
        conf.setPassword(new GuardedString(properties.getProperty("password").toCharArray()));
        conf.setServiceAddress(properties.getProperty("serviceAddress"));
        conf.setAuthMethod(properties.getProperty("authMethod"));
        conf.setTrustAllCertificates(Boolean.parseBoolean(properties.getProperty("trustAllCertificates")));

        conn = new EduIdConnector();
        conn.init(conf);

        DOMAIN = properties.getProperty("domain");
        ID = properties.getProperty("id");
    }

    @Test
    public void testConn() {
        conn.test();
    }

    @Test
    public void testSchema() {
        Schema schema = conn.schema();
        //LOG.info("schema: " + schema);
        // TODO: verify manually
    }

    public Optional<ConnectorObject> getByUid(String uid) throws IOException {
        EduIdFilter filter = new EduIdFilter(uid);
        List<ConnectorObject> results = new ArrayList<>();

            conn.executeQuery(affiliationObjectClass, filter, connectorObject -> {
                results.add(connectorObject);
                return true;
            }, null);
            return !results.isEmpty() ? Optional.of(results.getFirst()) : Optional.empty();
    }

    @Test
    public void testGetAll() throws IOException {
        List <ConnectorObject> results = new ArrayList<>();
        conn.executeQuery(affiliationObjectClass, null, connectorObject -> {
           results.add(connectorObject);
            return true;
        }, null);
            Assert.assertFalse(results.isEmpty(), "Expected to find at least one affiliation");
    }

    @Test(expectedExceptions = {org.identityconnectors.framework.common.exceptions.UnknownUidException.class})
    public void testGetNonExisting() throws IOException {
        String nonExistingUid = "idThatDoesNotExist" + DOMAIN;
        EduIdFilter filter = new EduIdFilter(nonExistingUid);
        assert getByUid(nonExistingUid).isEmpty(): "Expected not to find a user with UID: " + filter.byUid();
    }

    @Test
    public void testGetExisting() throws IOException {
        String existingUid = ID + DOMAIN;
        EduIdFilter filter = new EduIdFilter(existingUid);
        List<ConnectorObject> results = new ArrayList<>();
        assert getByUid(existingUid).isPresent() : "Expected to find an existing user with UID: " + filter.byUid();
    }

    @Test
    public void testUpdateExisting() throws IOException {
        // Update the existing user with new attributes
        Set<Attribute> attributes = new HashSet<Attribute>();
        String id = ID + DOMAIN;
        String[] emails = {"test@empa.ch"};
        attributes.add(AttributeBuilder.build("email", emails));
        conn.update(affiliationObjectClass, new Uid(id), attributes, null);
        ConnectorObject updatedObject = getByUid(id).orElseThrow(() -> new UnknownUidException("Expected to find an existing user with UID: " + id));
        assert updatedObject.getAttributeByName("email").getValue().contains(emails[0]) : "Expected email attribute to be updated to " + emails[0];

    }

    @Test
    public void testUpdateMfaPolicy() throws IOException {
        // Update the existing user with new attributes
        Set<Attribute> attributes = new HashSet<Attribute>();
        String id = ID + DOMAIN;
        attributes.add(AttributeBuilder.build("swissEduIDAffiliationSecurityPolicy.mfaPolicy.maxDeviceTrustDuration", "30D"));
        conn.update(affiliationObjectClass, new Uid(id), attributes, null);
        EduIdFilter filter = new EduIdFilter(ID + DOMAIN);
        List<ConnectorObject> results = new ArrayList<>();
        try {
            conn.executeQuery(affiliationObjectClass, filter, connectorObject -> {
                results.add(connectorObject);
                return true;
            }, null);
            assert results.size() == 1 : "Expected exactly one result, but got " + results.size();
        } catch (UnknownUidException e) {
            Assert.fail("Expected to find an existing user with UID: " + filter.byUid());
        }
        EduIdScimAffiliation aff = EduIdScimAffiliation.from(results.getFirst().getAttributes());
        System.out.println(aff);
        Assert.assertEquals(aff.swissEduIDAffiliationSecurityPolicy().mfaPolicy().maxDeviceTrustDuration(), "30D");

    }

}

