import io.github.pierresj.qiniufs.QiniuFileRegion
import io.github.pierresj.qiniufs.QiniuFileStorage
import io.github.pierresj.qiniufs.QiniuFileStorageConfiguration
import io.jmix.core.CoreConfiguration
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


    def "save open remove"(){
        def fileName=UuidProvider.createUuid().toString()+".txt";
        String string = "Text for testing qiniu.";
        InputStream inputStream = new ByteArrayInputStream(string.getBytes());
        def fileRef=fileStorage.saveStream(fileName,inputStream);
        def fileExists= fileStorage.fileExists(fileRef)
        def openedStream=fileStorage.openStream(fileRef);
        def fileOpened =openedStream!=null
        fileStorage.removeFile(fileRef)
        expect:
        verifyAll {
            fileExists
            fileOpened
        }
    }

    def "storage initialized"() {
        expect:
        fileStorage.getStorageName() == QiniuFileStorage.DEFAULT_STORAGE_NAME
    }
}