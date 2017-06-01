package jaemisseo.man

import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Created by sujkim on 2017-02-08.
 */
class FileManTest {

    @Before
    void init(){
    }

    @After
    void after(){
    }

//    static void main(String[] args) {
//        new FileManTest().findAllFile()
//    }

    @Test
    @Ignore
    void isRoot(){
        assert FileMan.isRootPath('/d')
        assert FileMan.isRootPath('/d/')
        assert FileMan.isRootPath('/d////')
        assert FileMan.isRootPath('/d////\\\\')
        assert FileMan.isRootPath('d/') == false
        assert FileMan.isRootPath('/')
        assert FileMan.isRootPath('') == false
        assert FileMan.isRootPath('c/') == false
        assert FileMan.isRootPath('/c/')
        assert FileMan.isRootPath('c:/')
        assert FileMan.isRootPath('c:\\')
        assert FileMan.isRootPath('p:\\') == false
        assert FileMan.isRootPath('gh:\\') == false
    }

    @Test
    @Ignore
    void startWithRoot(){
        assert FileMan.isItStartsWithRootPath('/d')
        assert FileMan.isItStartsWithRootPath('/d/')
        assert FileMan.isItStartsWithRootPath('d/')
        assert FileMan.isItStartsWithRootPath('/o')
    }

    @Test
    @Ignore
    void findAllFile(){
        //Ready
        String rootPath = '/'
        String fileName = 'meta.properties'
        def condition = [
            'logback.xml'   : true,
            '../../WEB-INF' : false,
        ]


        List<File> foundFileListTemp = FileMan.findAll(rootPath, fileName, condition)
        println "${foundFileListTemp.size()}건이 검색되었습니다."

        int i = 0;
        List<File> foundFileList = FileMan.findAll(rootPath, fileName, condition) { File foundFile ->
            println "${++i}) ${foundFile}"
            return true
        }

        println "${foundFileList.size()}건이 검색되었습니다."
    }


}
