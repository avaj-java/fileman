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
//        new FileManTest().findAllFileWithProgressBar()
//    }

    @Test
    @Ignore
    void isRootPath(){
        List correctList = ['/d', '/d/', '/d////', '/d////\\\\', '/', '/c/', 'c:/', 'c:\\']
        List wrongList = ['c', 'd', 'd/', '', 'c/', 'p:\\', 'gh:\\', 'asdfasf', 'ccc', 'c:c']

        correctList.each{
            assert FileMan.isRootPath(it)
        }
        wrongList.each{
            assert FileMan.isRootPath(it) == false
        }
    }

    @Test
    @Ignore
    void startsWithRootPath(){
        List correctList = ['/d', '/d/', '/d////', '/d////\\\\', '/', '/c/', 'c:/', 'c:\\']
        List wrongList = ['c', 'd', 'd/', '', 'c/', 'p:\\', 'gh:\\', 'asdfasf', 'ccc', 'c:c']

        correctList.each{
            assert FileMan.startsWithRootPath(it)
        }
        wrongList.each{
            assert FileMan.startsWithRootPath(it) == false
        }
    }

    @Test
    @Ignore
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

        //RealTime Listing Finder
        int i = 0;
        List<File> foundFileList = FileMan.findAll(rootPath, fileName, condition){ File foundFile ->
            println "${++i}) ${FileMan.getFullPath(foundFile.path, editResultPath)}"
            return true
        }
        println "${foundFileList.size()} was founded"
    }

    @Test
    @Ignore
    void findAllFileWithProgressBar(){
        //Ready
        String rootPath = '/'
        String fileName = 'rebel.xml'
        String editResultPath = '../../'
        def condition = [
            'logback.xml'   : true,
            '../../WEB-INF' : true,
        ]

        //RealTime Listing Finder
        List<File> foundFileList = FileMan.findAllWithProgressBar(rootPath, fileName, condition) { data ->
            int count = data.count
            (data.stringList as List) << "${count}) ${FileMan.getFullPath((data.item as File).path, editResultPath)}"
            return true
        }
        println "${foundFileList.size()} was founded"
    }

    @Test
    @Ignore
    void testGenEntryListFromZipFile(){
        String destPath = ''
        String filePath = ''
        List<String> entryList = FileMan.genEntryListFromZipFile(filePath)
        println FileMan.checkFiles(destPath, entryList, false)
    }

    @Test
    @Ignore
    void testGenEntryListFromJarFile(){
        String destPath = ''
        String filePath = ''
        List<String> entryList = FileMan.genEntryListFromJarFile(filePath)
        println FileMan.checkFiles(destPath, entryList, false)
    }

}
