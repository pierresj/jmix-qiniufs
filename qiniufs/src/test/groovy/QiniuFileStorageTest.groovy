import cn.stone.qiniufs.QiniuFileRegion
import cn.stone.qiniufs.QiniuFileStorage
import cn.stone.qiniufs.QiniuFileStorageConfiguration
import io.jmix.core.CoreConfiguration
import io.jmix.core.FileRef
import io.jmix.core.FileStorage
import io.jmix.core.UuidProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import test_support.QiniuFileStorageTestConfiguration
import test_support.TestContextInititalizer

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ContextConfiguration(
        classes = [CoreConfiguration, QiniuFileStorageConfiguration,QiniuFileStorageTestConfiguration],
        initializers = [TestContextInititalizer]
)
class QiniuFileStorageTest extends Specification {

    @Autowired
    private FileStorage fileStorage
    @Autowired
    private QiniuFileRegion qiniuFileRegion;


    def "save stream"(){
        def fileName=UuidProvider.createUuid().toString()+".txt";
        def fileStream=this.getClass().getClassLoader().getResourceAsStream("files/simple.txt");
        //println fileStream
        def fileRef=fileStorage.saveStream(fileName,fileStream);
        def openedStream=fileStorage.openStream(fileRef);
        expect:
            openedStream!=null
    }

    def "openStream"(){
        def fileKey = "2021/11/13/ff5398c4-2571-fc67-64c3-f874073067dc.txt"
        def fileName="ff5398c4-2571-fc67-64c3-f874073067dc.txt"
        def storageName = fileStorage.getStorageName()
        def fileRef = new FileRef(storageName, fileKey, fileName)
        fileStorage.openStream(fileRef)

        expect: true
    }

    def "fileExists"() {
        def storageName = fileStorage.getStorageName()
        def fileKey = "2021/11/13/ff5398c4-2571-fc67-64c3-f874073067dc.txt"
        def fileName="ff5398c4-2571-fc67-64c3-f874073067dc.txt"

        def fileref = new FileRef(storageName, fileKey, fileName)
        def exists = fileStorage.fileExists(fileref)

        expect:  exists

    }


    def "removeFile"(){
        def storageName = fileStorage.getStorageName()
        def fileKey = "2021/11/12/583fa259-848e-0ad3-8888-7d6be6e6f203.txt"
        def fileName="583fa259-848e-0ad3-8888-7d6be6e6f203.txt"

        def fileref = new FileRef(storageName, fileKey, fileName);
        fileStorage.removeFile(fileref)

        def exists = fileStorage.fileExists(fileref)

        expect:  !exists
    }

    def "getRegion"(){
        def region = qiniuFileRegion.getRegion("");
        println region
        expect: true
    }

    def "ali storage initialized"() {
        expect:
        fileStorage.getStorageName() == QiniuFileStorage.DEFAULT_STORAGE_NAME
    }
}