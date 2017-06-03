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
    void isRootPath(){
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
    void isItStartsWithRootPath(){
        assert FileMan.isItStartsWithRootPath('/d')
        assert FileMan.isItStartsWithRootPath('/d/')
        assert FileMan.isItStartsWithRootPath('d/')
        assert FileMan.isItStartsWithRootPath('/o')
    }

    @Test
    void getRelativePath(){
        String a, b, relpath

        // One Difference
        a = "/workspace/project/build/installer_myproject/bin"
        b = "/workspace/project/build/installer_myproject"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == ".."

        a = "d/workspace/project/build/installer_myproject/bin"
        b = "d/workspace/project/build/installer_myproject"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == ".."

        a = "d:/workspace/project/build/installer_myproject/bin/"
        b = "d:/workspace/project/build/installer_myproject/"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == ".."

        // Same
        a = "d:/workspace/project/build/installer_myproject/"
        b = "d:/workspace/project/build/installer_myproject/"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == "."

        // Minus and Plus
        a = "d:/ddd"
        b = "d:/workspace/project/build/installer_myproject/"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == "../workspace/project/build/installer_myproject"

        // Different Driver Name
        a = "/"
        b = "d:/workspace/project/build/installer_myproject"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == "d:/workspace/project/build/installer_myproject"

        a = "c:/fasdf/asdfff/fff"
        b = "d:/workspace/project/build/installer_myproject"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == "d:/workspace/project/build/installer_myproject"

        a = "f:/sassss/asdsss/ssss"
        b = "c:/workspace/project/build/installer_myproject"
        relpath = FileMan.getRelativePath(a, b)
        assert relpath == "c:/workspace/project/build/installer_myproject"
    }

    @Test
    @Ignore
    void findAllFile(){
        //Ready
        String rootPath = '/'
        String fileName = 'rebel.xml'
        String editResultPath = '../../'
        def condition = [
            'logback.xml'   : true,
            '../../WEB-INF' : true,
        ]

        //Simple Finder
        List<File> foundFileListTemp = FileMan.findAll(rootPath, fileName, condition)
        println "${foundFileListTemp.size()} was founded"

        //RealTime Listing Finder
        int i = 0;
        List<File> foundFileList = FileMan.findAll(rootPath, fileName, condition) { File foundFile ->
            String editedPath = FileMan.getFullPath(foundFile.path, editResultPath)
            println "${++i}) ${editedPath}"
            return true
        }
        println "${foundFileList.size()} was founded"
    }


}
