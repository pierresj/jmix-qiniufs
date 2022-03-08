package io.github.pierresj.qiniufs;

import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@ManagedResource(description = "Manages Qiniu Kodo file storage client", objectName = "jmix.qiniufs:type=QiniuFileStorage")
@Component("qiniufs_QiniuFileStorageManagementFacade")
public class QiniuFileStorageManagementFacade {
    @Autowired
    protected FileStorageLocator fileStorageLocator;

    @ManagedOperation(description = "Refresh Qiniu Kodo file storage client")
    public String refreshQiniuOssClient() {
        FileStorage fileStorage = fileStorageLocator.getDefault();
        if (fileStorage instanceof QiniuFileStorage) {
            ((QiniuFileStorage) fileStorage).refreshKodoClient();
            return "Refreshed successfully";
        }
        return "Not an Qiniu file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh Qiniu Kodo file storage client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "Qiniu Kodo access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "Qiniu Kodo secret access key")})
    public String refreshQiniuKodoClient(String storageName, String accessKey, String secretAccessKey) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof QiniuFileStorage) {
            QiniuFileStorage qiniuFileStorage = (QiniuFileStorage) fileStorage;
            qiniuFileStorage.setAccessKey(accessKey);
            qiniuFileStorage.setSecretAccessKey(secretAccessKey);
            qiniuFileStorage.refreshKodoClient();
            return "Refreshed successfully";
        }
        return "Not an Qiniu Kodo file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh Qiniu Kodo file storage client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "Qiniu Kodo access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "Qiniu Kodo secret access key"),
            @ManagedOperationParameter(name = "bucket", description = "Qiniu Kodo bucket name"),
            @ManagedOperationParameter(name = "chunkSize", description = "Qiniu Kodo chunk size (kB)"),
            @ManagedOperationParameter(name = "regionName", description = "Qiniu Kodo region"),
            @ManagedOperationParameter(name = "endpointUrl", description = "Optional custom Qiniu Kodo storage endpoint URL")})
    public String refreshQiniuKodoClient(String storageName, String accessKey, String secretAccessKey,
                                  String regionName, String bucket, int chunkSize, @Nullable String endpointUrl) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof QiniuFileStorage) {
            QiniuFileStorage qiniuFileStorage = (QiniuFileStorage) fileStorage;
            qiniuFileStorage.setAccessKey(accessKey);
            qiniuFileStorage.setSecretAccessKey(secretAccessKey);
            qiniuFileStorage.setRegionName(regionName);
            qiniuFileStorage.setBucket(bucket);
            qiniuFileStorage.setChunkSize(chunkSize);
            qiniuFileStorage.setEndpointUrl(endpointUrl);
            qiniuFileStorage.refreshKodoClient();
            return "Refreshed successfully";
        }
        return "Not an Qiniu Kodo file storage - refresh attempt ignored";
    }
}
