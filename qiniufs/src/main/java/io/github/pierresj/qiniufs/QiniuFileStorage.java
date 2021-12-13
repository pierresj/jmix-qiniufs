package io.github.pierresj.qiniufs;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.*;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.persistent.FileRecorder;
import com.qiniu.util.Auth;
import io.jmix.core.*;
import io.jmix.core.annotation.Internal;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import com.google.gson.*;

@Internal
@Component("qiniufs_FileStorage")
public class QiniuFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(QiniuFileStorage.class);
    public static final String DEFAULT_STORAGE_NAME = "qiniukodo";

    protected String storageName;

    @Autowired
    protected QiniuFileStorageProperties properties;

    boolean useConfigurationProperties = true;

    protected String accessKey;
    protected String secretAccessKey;
    protected String bucket;
    protected int chunkSize;
    protected String endpointUrl;
    protected String regionName;
    protected Region region; //机房

    @Autowired
    protected TimeSource timeSource;
    @Autowired
    private QiniuFileRegion qiniuFileRegion;

    protected AtomicReference<Auth> clientReference = new AtomicReference<>();

    public QiniuFileStorage() {
        this(DEFAULT_STORAGE_NAME);
    }

    public QiniuFileStorage(String storageName) {
        this.storageName = storageName;
    }

    /**
     * Optional constructor that allows you to override {@link QiniuFileStorageProperties}.
     */
    public QiniuFileStorage(String storageName,
                            String accessKey,
                            String secretAccessKey,
                            String bucket,
                            int chunkSize,
                            @Nullable String endpointUrl,
                            String regionName) {
        this.useConfigurationProperties = false;
        this.storageName = storageName;
        this.accessKey = accessKey;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
        this.chunkSize = chunkSize;
        this.endpointUrl = endpointUrl;
        this.region = qiniuFileRegion.getRegion(regionName);
    }

    @EventListener
    public void initOssClient(ApplicationStartedEvent event) {
        refreshOssClient();
    }

    protected void refreshProperties() {
        if (useConfigurationProperties) {
            this.accessKey = properties.getAccessKey();
            this.secretAccessKey = properties.getSecretAccessKey();
            this.bucket = properties.getBucket();
            this.chunkSize = properties.getChunkSize();
            this.endpointUrl = properties.getEndpointUrl();
        }
    }

    public void refreshOssClient() {
        refreshProperties();
        Auth auth = Auth.create(accessKey, secretAccessKey);
        clientReference.set(auth);
    }

    @Override
    public String getStorageName() {
        return storageName;
    }

    protected String createFileKey(String fileName) {
        return createDateDir() + "/" + createUuidFilename(fileName);
    }

    protected String createDateDir() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timeSource.currentTimestamp());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return String.format("%d/%s/%s", year,
                StringUtils.leftPad(String.valueOf(month), 2, '0'),
                StringUtils.leftPad(String.valueOf(day), 2, '0'));
    }

    protected String createUuidFilename(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isNotEmpty(extension)) {
            return UuidProvider.createUuid().toString() + "." + extension;
        } else {
            return UuidProvider.createUuid().toString();
        }
    }

    @Override
    public FileRef saveStream(String fileName, InputStream inputStream) {
        String key = createFileKey(fileName); //文件名
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(this.region);//机房
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;// 指定分片上传版本
        cfg.resumableUploadMaxConcurrentTaskCount = 2;  // 设置分片上传并发，1：采用同步上传；大于1：采用并发上传
        cfg.resumableUploadAPIV2BlockSize = this.chunkSize * 1024; //使用分片 V2 上传时的分片大小

        try {
            byte[] data = IOUtils.toByteArray(inputStream); //数据流

            Auth auth = Auth.create(accessKey, secretAccessKey);
            String upToken = auth.uploadToken(bucket);
            String localTempDir = Paths.get(System.getenv("java.io.tmpdir"), bucket).toString();
            //设置断点续传文件进度保存目录
            FileRecorder fileRecorder = new FileRecorder(localTempDir);
            UploadManager uploadManager = new UploadManager(cfg, fileRecorder);
            Response response = uploadManager.put(data, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);

            return new FileRef(getStorageName(), putRet.key, fileName);
        } catch (IOException e) {
            String message = String.format("Could not save file %s.", fileName);
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, e.getMessage());
        }
    }

    protected byte[] getChunkBytes(byte[] data, int start, int end) {
        byte[] chunkBytes = new byte[end - start];
        System.arraycopy(data, start, chunkBytes, 0, end - start);
        return chunkBytes;
    }

    @Override
    public InputStream openStream(FileRef reference) {
        String domain = this.endpointUrl;
        String key = reference.getPath();
        DownloadUrl url = new DownloadUrl(domain, false, key);
        try {
            String urlString = url.buildURL();//文件链接
            URL urlNet = new URL(urlString);
            URLConnection conn = urlNet.openConnection();
            return conn.getInputStream();
        } catch (Exception e) {
            String message = String.format("Could not load file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public void removeFile(FileRef reference) {
        Configuration cfg = new Configuration(Region.region1());

        Auth auth = Auth.create(accessKey, secretAccessKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            bucketManager.delete(bucket, reference.getPath());
        } catch (Exception e) {
            String message = String.format("Could not delete file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public boolean fileExists(FileRef reference) {
        Configuration cfg = new Configuration(Region.region1());

        Auth auth = Auth.create(accessKey, secretAccessKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            FileInfo fileInfo = bucketManager.stat(bucket, reference.getPath());
            return fileInfo.fsize > 0;
        } catch (QiniuException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setEndpointUrl(@Nullable String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
}
