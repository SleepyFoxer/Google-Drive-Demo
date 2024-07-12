package org.zeasn.factories;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import org.zeasn.constants.Constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Date;

public class OAuthFactory {
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(OAuthFactory.class.getResource("/").getPath(), "credentials");
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * 获取Credentials
     *
     * @param serverAuthCode 客户端授权获取
     * @return Credentials
     */
    public static Credentials getCredentials(String serverAuthCode) throws GeneralSecurityException, IOException {
        // 这里就是读取在Google Console下载的JSON文件，作为请求凭据
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JSON_FACTORY, new InputStreamReader(new FileInputStream(Constants.CREDENTIAL_PATH)));
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        JSON_FACTORY,
                        clientSecrets,
                        // 这里还是授权范围，相册的话是
                        // "https://www.googleapis.com/auth/photoslibrary.readonly",
                        // "https://www.googleapis.com/auth/photoslibrary.appendonly"
                        Constants.SCOPES)
                        //缓存数据，可继承AbstractDataStoreFactory自定义缓存的Credential位置
                        .setDataStoreFactory(new FileDataStoreFactory(DATA_STORE_DIR))
                        // 设置offline，换取的Credential会返回refreshToken
                        .setAccessType("offline")
                        .build();

        // 先从本地缓存获取对应的Credential，loadCredential入参正常是userId，此Demo暂时用serverAuthCode替代
        Credential credential = flow.loadCredential(serverAuthCode);

        boolean isGet = false; // 判断本地是否有对应的Credential以及是否刷新过refreshToken
        if (credential != null) {
            // 注意每个账号只有在第一次获取Credential才会返回refreshToken（有待实际场景验证）
            // Credential持有一个有效时间很短的accessToken和一个有效时间6个月左右的refreshToken，各自作用顾名思义
            // 建议记住token的失效时间，在失效时间到来之前对token进行刷新
            // 若连refreshToken都失效，那只能客户端重新授权获取到serverAuthCode后再执行Credential获取操作
            isGet = credential.refreshToken();
        }
        if (!isGet) {
            // 通过serverAuthCode获取Credential
            GoogleTokenResponse googleTokenResponse = flow.newTokenRequest(serverAuthCode).execute();
            // 转换获取Credential并保存到本地，createAndStoreCredential入参正常是GoogleTokenResponse、userId，此Demo暂时用serverAuthCode替代
            credential = flow.createAndStoreCredential(googleTokenResponse, serverAuthCode);
        }
        return UserCredentials.newBuilder()
                .setClientId(clientSecrets.getDetails().getClientId())
                .setClientSecret(clientSecrets.getDetails().getClientSecret())
                .setAccessToken(AccessToken.newBuilder()
                        .setTokenValue(credential.getAccessToken())
                        .setExpirationTime(new Date(credential.getExpirationTimeMilliseconds()))
                        .setScopes(Constants.SCOPES)
                        .build())
                .setRefreshToken(credential.getRefreshToken())
                .build();
    }
}
