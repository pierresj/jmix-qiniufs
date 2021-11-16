package cn.stone.qiniufs;

import com.qiniu.storage.Region;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("qiniufs_FileRegion")
public class QiniuFileRegion {

    public QiniuFileRegion() {
    }

    public QiniuFileRegion(String storageName) {

    }

    public Region getRegion(String region){
        Map<String, Region> regionMap = new HashMap();
        regionMap.put("huadong", Region.huadong());
        regionMap.put("huabei", Region.huabei());
        regionMap.put("huanan", Region.huanan());
        regionMap.put("beimei", Region.beimei());
        regionMap.put("xinjiapo", Region.xinjiapo());
        if(null == regionMap.get(region)){
            return Region.autoRegion();
        }
        return  regionMap.get(region);
    }
}
