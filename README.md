# Jmix Qiniu Kodo File Storage

用于七牛云对象存储

## 安装

| Jmix Version | Add-on Version | Implementation |
|:-|:-|:-|
| 1.1.* |1.0.2|io.github.pierresj:jmix-qiniufs-starter:1.0.2|

### build.gradle
```
implementation 'io.github.pierresj:jmix-qiniufs-starter:1.0.2'
```
### 配置参数(以properties文件为例)
```properties
jmix.core.defaultFileStorage=qiniu_fs

jmix.qiniuifs.accessKey=your accessKey
jmix.qiniufs.secretAccessKey=your secretAccessKey
jmix.qiniufs.bucket=your bucket
jmix.qiniufs.chunkSize=${qiniufs.chunkSize}
jmix.qiniufs.endpointUrl=${qiniufs.endpointUrl}
jmix.qiniufs.reginName=${qiniufs.reginName}
```
### 例子
```
@Autowired
private FileStorage fileStorage;

File file = temporaryStorage.getFile(manuallyControlledField.getFileId());
InputStream fileInputStream = new FileInputStream(file);        
FileRef fileRef = fileStorage.saveStream(event.getFileName(), fileInputStream);
```
