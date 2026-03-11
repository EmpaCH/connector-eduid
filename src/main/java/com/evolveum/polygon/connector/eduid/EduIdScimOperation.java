package com.evolveum.polygon.connector.eduid;

import org.apache.http.client.methods.HttpRequestBase;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;

sealed interface EduIdScimOperation permits EduIdScimOperation.Get, EduIdScimOperation.GetAll, EduIdScimOperation.Post,
        EduIdScimOperation.Put, EduIdScimOperation.Delete {
    record Get(String uid)    implements EduIdScimOperation {}
    record GetAll()          implements EduIdScimOperation {}
    record Post()             implements EduIdScimOperation {}
    record Put(String uid)    implements EduIdScimOperation {}
    record Delete(String uid) implements EduIdScimOperation {}

    static EduIdScimOperation from(HttpRequestBase request) {
        String path = request.getURI().getPath();
        String uid = path.replaceFirst(".*/Affiliations/", "");
        uid = uid.equals(path) ? null : uid; // null if no uid segment
        String finalUid = uid;
        return switch (request.getMethod()) {
            case "GET"   -> {
                if (finalUid == null) {
                    yield new GetAll();
                } else {
                    yield new Get(finalUid);
                }
            }
            case "POST"   -> new Post();
            case "PUT"    -> new Put(uid);
            case "DELETE" -> new Delete(uid);
            default -> throw new ConnectorIOException("Unknown HTTP method: " + request.getMethod());
        };
    }
}