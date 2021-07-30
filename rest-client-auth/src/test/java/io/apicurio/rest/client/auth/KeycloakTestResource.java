package io.apicurio.rest.client.auth;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.List;

public class KeycloakTestResource {

    private KeycloakContainer container;
    String testUsername = "sr-test-user";
    String testPassword = "sr-test-password";


    public void start() {
        container = new KeycloakContainer()
                .withRealmImportFile("test-realm.json");
        container.start();
        createTestUser();
    }

    public String getAuthServerUrl() {
        return container.getAuthServerUrl() + "/realms/" + "registry" ;
    }

    private void createTestUser() {
        Keycloak keycloakAdminClient = KeycloakBuilder.builder()
                .serverUrl(container.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(container.getAdminUsername())
                .password(container.getAdminPassword())
                .build();

        final UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(testUsername);
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);

        final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(testPassword);
        credentialRepresentation.setTemporary(false);

        userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));

        keycloakAdminClient.realm("registry")
                .users()
                .create(userRepresentation);

        final RoleRepresentation adminRoleRepresentation = keycloakAdminClient.realm("registry")
                .roles()
                .get("sr-admin")
                .toRepresentation();

        final UserRepresentation user = keycloakAdminClient.realm("registry")
                .users()
                .search(testUsername)
                .get(0);

        keycloakAdminClient.realm("registry")
                .users()
                .get(user.getId())
                .roles()
                .realmLevel()
                .add(List.of(adminRoleRepresentation));
    }

    public void stop() {
        container.stop();
        container.close();
    }
}
