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
    void getSubFilePathList(){
        List fileSubPathList13 = FileMan.getSubFilePathList('/')
        List fileSubPathList14 = FileMan.getSubFilePathList('/*')

        List fileSubPathList = FileMan.getSubFilePathList('d:')
        List fileSubPathList4 = FileMan.getSubFilePathList('d:/')
        List fileSubPathList7 = FileMan.getSubFilePathList('d:/*')
        List fileSubPathList10 = FileMan.getSubFilePathList('d:/*.yml')

        List fileSubPathList2 = FileMan.getSubFilePathList('d:/dev_by_sj')
        List fileSubPathList5 = FileMan.getSubFilePathList('d:/dev_by_sj/')
        List fileSubPathList8 = FileMan.getSubFilePathList('d:/dev_by_sj/*')
        List fileSubPathList11 = FileMan.getSubFilePathList('d:/dev_by_sj/t*')

        List fileSubPathList3 = FileMan.getSubFilePathList('d:/dev_by_sj/dmps')
        List fileSubPathList6 = FileMan.getSubFilePathList('d:/dev_by_sj/dmps/')
        List fileSubPathList9 = FileMan.getSubFilePathList('d:/dev_by_sj/dmps/*')
        List fileSubPathList12 = FileMan.getSubFilePathList('d:/dev_by_sj/dmps/u*')
        println 1
    }

    @Test
    @Ignore
    void isMatchedPath(){
//        List fileEntryPathList = FileMan.genEntryList('d:/')
        List fileEntryPathList2 = FileMan.genEntryList('d:/dow*')
        List fileEntryPathList3 = FileMan.genEntryList('d:/dev_by_sj/dmps')
        List fileEntryPathList4 = FileMan.genEntryList('d:/dev_by_sj/dmps/')
        List fileEntryPathList5 = FileMan.genEntryList('d:/dev_by_sj/dmps/*')
        println 1
    }



    @Test
    void isMatchingFile(){
        List<String> filePathList = [
            'c:/test_on_cdrive/not_real_path/just-test',
            'c:/test_on_cdrive/not_real_path/just-test/dd',
            'c:/test_on_cdrive/not_real_path/just-test/hello.yml',
            'c:/test_on_cdrive/not_real_path/just-test/bye.yml',
            'd:/test/not_real_path/just-test',
            'd:/test/not_real_path/just-test/dd',
            'd:/test/not_real_path/just-test/hello.yml',
            'd:/test/not_real_path/just-test/bye.yml',
            'd:/test/not_real_path/just-test/installer-maker.yml',
            'd:/test/not_real_path/just-test/installer.yml',
            'd:/test/not_real_path/just-test/build.sh',
            'd:/test/not_real_path/just-test/build.bat',
            'd:/test/not_real_path2/just-test/.git',
            'd:/test/not_real_path2/just-test/.git/git.config',
            'd:/test/not_real_path2/just-test/.gitignore',
            'd:/test/not_real_path2/just-test/build',
            'd:/test/not_real_path2/just-test/build/installer_myproject',
            'd:/test/not_real_path2/just-test/build/installer_myproject/bin',
            'd:/test/not_real_path2/just-test/build/installer_myproject/bin/install',
            'd:/test/not_real_path2/just-test/build/installer_myproject/bin/install.bat',
            'd:/test/not_real_path2/just-test/build/installer_myproject/bin/macgyver',
            'd:/test/not_real_path2/just-test/build/installer_myproject/bin/macgyver.bat',
            'd:/test/not_real_path2/just-test/build/installer_myproject/log',
            'd:/test/not_real_path2/just-test/build/installer_myproject/lib',
        ]
        assert isMachingData(filePathList, 'd:/t*t/not_real_path/just-test', [
            'd:/test/not_real_path/just-test'
        ])

        assert isMachingData(filePathList, 'd:/*/not_real_path/just-test', [
                'd:/test/not_real_path/just-test'
        ])

        assert isMachingData(filePathList, 'd:/*/n*a*h/just-test/*.yml', [
                'd:/test/not_real_path/just-test/hello.yml',
                'd:/test/not_real_path/just-test/bye.yml',
                'd:/test/not_real_path/just-test/installer-maker.yml',
                'd:/test/not_real_path/just-test/installer.yml'
        ])

        assert isMachingData(filePathList, 'd:/**/just-test', [
                'd:/test/not_real_path/just-test'
        ])

        assert isMachingData(filePathList, 'd:/**', filePathList.findAll{ it.startsWith('d:') })

        assert isMachingData(filePathList, 'c:/**', filePathList.findAll{ it.startsWith('c:') })
    }

    private boolean isMachingData(List<String> dataList, String range, List<String> assertDataList){
        boolean isSameSize = true
        boolean containsAll = true
        List<String> filteredDataList = dataList.findAll{ FileMan.isMatchedPath(it, range) }
        //Log
        println "${range} "
        println "  => ${filteredDataList}"
        //Check
        if (filteredDataList){
            isSameSize = (assertDataList.size() == filteredDataList.size())
            containsAll = assertDataList.every{ filteredDataList.contains(it) }
        }else{
            isSameSize = (assertDataList.size() == 0)
        }
        return isSameSize && containsAll
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
