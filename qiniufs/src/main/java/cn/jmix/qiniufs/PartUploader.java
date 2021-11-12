package cn.jmix.qiniufs;

import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import io.jmix.core.FileStorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class PartUploader implements Runnable {

    private final String key;
    private final byte[] data; //文件流
    private final String upToken;

    public PartUploader(String key, byte[] data, String upToken) {
        this.key = key;
        this.data = data;
        this.upToken = upToken;
    }

    @Override
    public void run() {
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(this.data);



        } catch (Exception e) {
          throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,"uploading a part of data failed",e);
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }





}
