package org.zeasn;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.zeasn.constants.Constants;
import org.zeasn.factories.DriveServiceFactory;

import java.io.*;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriveClient {
    private static String backupId = "1lpo7wtbMctjvLLRpx0XAX1t0nS5ySzc8"; // 备份文件夹ID，实际开发场景应与用户绑定保存
    private static final Map<String, String> extraProperties = Map.of("type", "uhale_backup");

    public static void main(String[] args) {
        try {
            Drive drive = DriveServiceFactory.getDriveService();

//            createFolder(drive,"Uhale-backup");
//            uploadFiles(drive, backupId, Constants.TEST_SRC_DIR);
//            getFiles(drive, backupId);
//            getStorage(drive);
//            downloadFiles(drive, backupId, Constants.TEST_DST_DIR);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建文件夹
     *
     * @param drive      云盘
     * @param folderName 文件名
     * @return 创建成功的文件夹ID
     */
    public static String createFolder(Drive drive, String folderName) throws GoogleJsonResponseException, IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType(Constants.MIME_TYPE_FOLDER);
        File file = drive.files().create(fileMetadata)
                .setFields("id,name")
                .execute();
        backupId = file.getId();
        System.out.println("Folder ID: " + file.getId() + " name: " + file.getName());
        return file.getId();
    }

    /**
     * 上传多个文件
     *
     * @param drive     云盘
     * @param backupId  上传到云端的文件目录ID
     * @param srcFolder 本地待上传源文件夹
     */
    public static void uploadFiles(Drive drive, String backupId, String srcFolder) throws IOException {
        java.io.File parentFile = new java.io.File(srcFolder);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (java.io.File file : Objects.requireNonNull(parentFile.listFiles())) {
            executor.submit(() -> {
                try {
                    uploadFile(drive, backupId, file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
    }

    /**
     * 上传单个文件
     *
     * @param drive    云盘
     * @param backupId 上传到云端的文件目录ID
     * @param file     本地待上传文件
     */
    public static void uploadFile(Drive drive, String backupId, java.io.File file) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(file.getName());
        // 设置备份在哪个目录下
        fileMetadata.setParents(Collections.singletonList(backupId));
        // 设置额外属性，以便于后面获取文件列表筛选使用
        fileMetadata.setAppProperties(extraProperties);
        FileContent mediaContent = new FileContent(URLConnection.guessContentTypeFromName(file.getName()), file);
        File result = drive.files().create(fileMetadata, mediaContent)
                .setFields("id,name,appProperties")
                .execute();
        System.out.println("Folder ID: " + result.getId() + " name: " + result.getName());
    }

    /**
     * 下载文件
     *
     * @param drive    云盘
     * @param backupId 下载的云端文件目录ID
     * @param dstPath  本地文件下载保存的文件夹路径
     */
    public static void downloadFiles(Drive drive, String backupId, String dstPath) throws IOException {
        java.io.File dstFolder = new java.io.File(dstPath);
        dstFolder.mkdirs();

        // 获取目录下所有文件
        List<File> files = getFiles(drive, backupId);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (File file : files) {
            executor.submit(() -> {
                try {
                    downloadFile(drive, file, dstFolder);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
    }

    /**
     * 下载单个文件
     *
     * @param drive     云盘
     * @param file      云文件信息
     * @param dstFolder 本地文件下载保存的文件夹
     */
    public static void downloadFile(Drive drive, File file, java.io.File dstFolder) throws IOException {
        java.io.File dstFile = new java.io.File(dstFolder, file.getName());
        FileOutputStream fos = new FileOutputStream(dstFile);
        drive.files().get(file.getId())
                .setFields("id,name")
                .executeMediaAndDownloadTo(fos);
        fos.flush();
        fos.close();
    }

    /**
     * 获取文件数据
     *
     * @param drive    云盘
     * @param backupId 云端文件目录ID
     * @return 返回获取的备份数据
     */
    public static List<File> getFiles(Drive drive, String backupId) throws IOException {
        // 获取文件夹下的所有文件
        String pageToken = null;
        List<File> files = new ArrayList<>();

        // 添加搜索条件
        // 是备份文件、没被放回回收站且是我们备份额外设置的属性即extraProperties
        StringBuilder filter = new StringBuilder();
        filter.append("'").append(backupId).append("' in parents and trashed=false");
        extraProperties.forEach((key, value) -> {
            filter.append(" and appProperties has { key='").append(key).append("' and value='").append(value).append("' }");
        });

        do {
            FileList result = drive.files().list()
                    // 添加搜索条件
                    .setQ(filter.toString())
                    .setPageSize(10)
                    .setPageToken(pageToken)
                    // thumbnailLink通常只有几个小时的有效期，具体时间不清
                    .setFields("nextPageToken, files(id, name, thumbnailLink)")
                    .execute();
            files.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        files.forEach(file -> {
            System.out.println("Folder ID: " + file.getId() + " name: " + file.getName());
            System.out.println("thumbnailLink: " + file.getThumbnailLink());
        });
        return files;
    }

    /**
     * 更新文件
     *
     * @param drive  云盘
     * @param fileId 云端文件ID
     * @param file   需要修改的文件属性
     * @return 更新之后的文件
     */
    public static File updateFile(Drive drive, String fileId, File file) throws IOException {
        return drive.files().update(fileId, file).execute();
    }

    /**
     * 删除文件
     *
     * @param drive  云盘
     * @param fileId 云端文件ID
     */
    public static void deleteFile(Drive drive, String fileId) throws IOException {
        drive.files().delete(fileId).execute();
    }

    /**
     * 获取存储空间信息
     *
     * @param drive 云盘
     * @return 存储空间信息
     */
    public static About.StorageQuota getStorage(Drive drive) throws Exception {
        About about = drive.about().get()
                .setFields("storageQuota")
                .execute();
        System.out.println(about.getStorageQuota().toString());
        return about.getStorageQuota();
    }

    /**
     * 监听文件夹变化
     *
     * @param drive    云盘
     * @param backupId 云端文件目录ID
     * @param callbackUrl 监听变化通知地址
     */
    public static void watch(Drive drive, String backupId,String callbackUrl) throws IOException {
        Channel channel = new Channel();
        channel.setId("test123"); // 唯一ID
        channel.setType("web_hook");
        channel.setAddress(callbackUrl);

        // 启动对文件夹变化的监视
        Channel resultChannel = drive.files().watch(backupId, channel).execute();
        System.out.println(resultChannel.toString());
    }
}
