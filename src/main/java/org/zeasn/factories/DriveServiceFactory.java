package org.zeasn.factories;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;

public class DriveServiceFactory {
    private static final String serverAuthCode = "4/0ATx3LY5OY-ErMsGrok2Sdzpq8uXO05yvKpm_XPm30Jn6osF5OtIQMUp4HM6NibfnvP27DQ";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * 获取云盘服务
     *
     * @return 云盘
     */
    public static Drive getDriveService() throws Exception {
        Credentials credentials = OAuthFactory.getCredentials(serverAuthCode);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                requestInitializer)
                .setApplicationName("Uhale")
                .build();
    }
}
