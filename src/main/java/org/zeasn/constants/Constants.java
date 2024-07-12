package org.zeasn.constants;

import com.google.api.services.drive.DriveScopes;

import java.util.Arrays;
import java.util.List;

public interface Constants {
    String TEST_SRC_DIR = "./test";
    String TEST_DST_DIR = "./recover";

    String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    String CLIENT_ID = "102626496449-qsfsh1ooa48h2t59c448jtndqd3q3c8n.apps.googleusercontent.com";
    String CREDENTIAL_PATH = "./client_secret.json";

    List<String> SCOPES = List.of(
            DriveScopes.DRIVE
    );
}
