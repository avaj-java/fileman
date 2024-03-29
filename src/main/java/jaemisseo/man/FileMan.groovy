package jaemisseo.man

import jaemisseo.man.bean.FileSetup
import jaemisseo.man.util.Util
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.CodeSource
import java.text.SimpleDateFormat
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Created by sujkim on 2017-02-19.
 */
class FileMan {

    FileMan(){}

    FileMan(String filePath){
        init()
        setSource(filePath)
    }

    FileMan(File file){
        init()
        setSource(file)
    }

    void init(){
        nowPath = System.getProperty('user.dir')
    }

    static final Logger logger = LoggerFactory.getLogger(this.getClass())

    boolean directoryAutoCreateUse = true

    final static int BUFFER = 2048
    static String nowPath = System.getProperty('user.dir')

    File sourceFile

    FileSetup globalOption = new FileSetup()

    String originalContent
    String content



    FileMan set(FileSetup fileSetup){
        globalOption.merge(fileSetup)
        return this
    }

    /*************************
     * Process - Backup
     *************************/
    boolean backupFiles(String dirPathToSave, String dirPathToBackup) throws Exception{
        backupFiles(dirPathToSave, dirPathToBackup, [], [])
    }

    boolean backupFiles(String dirPathToSave, String dirPathToBackup, List deleteExcludeList, List moveExcludeList) throws Exception{
        File dirForSave = new File(dirPathToSave)
        File dirForBackup = new File(dirPathToBackup)
        // 3-1. 기존 백업파일 지우기
        if (dirForBackup.exists())
            emptyDirectory(dirPathToBackup, deleteExcludeList)
        logger.debug('\n < FINISHED Delete > Backup Files')
        // 3-2. 디렉토리 체크 or 자동생성
        if (directoryAutoCreateUse){
            if ( !dirForSave.exists() && dirForSave.mkdirs() )
                logger.debug('\n < AUTO CREATE Directory > Save Directory')
            if ( !dirForBackup.exists() && dirForBackup.mkdirs() )
                logger.debug('\n < AUTO CREATE Directory > Backup Directory')
        }
        // 3-3. 기존 파일들을 -> 백업 디렉토리로 이동
        if ( dirForSave.exists() && dirForBackup.exists() ){
            logger.debug('\n < CHECK Directory > OK')
            if ( !moveAllInDirectory(dirPathToSave, dirPathToBackup, moveExcludeList) )
                throw new Exception('\n[MOVE Failed] Move Exist FIles To Backup Directory', new Throwable("You Need To Check Permission Check And... Some... "))
        }else{
            throw new Exception('\n[CHECK Failed] Some Directory Does Not Exists', new Throwable("Directory Exist Check - SaveDirectory:${dirForSave.exists()} / BackupDirectory:${dirForBackup.exists()}"))
        }
        logger.debug('\n < FINISHED Move > Move Exist FIles To Backup Directory')
        return true
    }

    /*************************
     * Read File
     *************************/
    List<String> getLineList(){
        return getLineList(sourceFile)
    }

    List<String> getLineList(String filePath){
        return getLineList(new File(filePath))
    }

    List<String> getLineList(String filePath, FileSetup opt){
        return getLineList(new File(filePath), opt)
    }

    List<String> getLineList(File f){
        return getLineList(f, globalOption)
    }

    List<String> getLineList(File f, FileSetup opt){
        opt = getMergedOption(opt)
        String encoding = opt.encoding
        List<String> lineList = new ArrayList<String>()
        String line
        try {
//            FileReader fr = new FileReader(f)
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), encoding)
            BufferedReader br = new BufferedReader(isr)
            while ((line = br.readLine()) != null) {
                lineList.add(line)
            }
//            fr.close()
            isr.close()
            br.close()

        } catch (Exception ex) {
            throw ex
        }

        return lineList
    }

    List<String> getLineList(File f, Long fromLineNumber) {
        return getLineList(f, fromLineNumber, null)
    }

    List<String> getLineList(File f, Long fromLineNumber, Long lineCount) {
        return getLineList(f, fromLineNumber, lineCount, globalOption)
    }

    List<String> getLineList(File f, Long fromLineNumber, Long lineCount, FileSetup opt) {
        opt = getMergedOption(opt)
        String encoding = opt.encoding
        List<String> lineList = new ArrayList<String>()
        fromLineNumber = fromLineNumber ?: 0
        String line
        InputStreamReader isr
        BufferedReader br
        try {
            isr = new InputStreamReader(new FileInputStream(f), encoding)
            br = new BufferedReader(isr)
            Long cnt = 0
            while ((line = br.readLine()) != null) {
                ++cnt
                if (lineCount != null && fromLineNumber + (lineCount-1) < cnt)
                    break
                if ( cnt >= fromLineNumber)
                    lineList.add(line)
            }

        } catch (Exception ex) {
            throw ex
        }finally{
            if (isr)
                isr.close()
            if (br)
                br.close()
        }

        return lineList
    }

    /*************************
     * Create New File By LineList
     *************************/
    boolean createNewFile(String dirPath, String fileName, List fileContentLineList){
        createNewFile(dirPath, fileName, fileContentLineList, new FileSetup())
    }

    boolean createNewFile(String dirPath, String fileName, List fileContentLineList, FileSetup fileSetup){
        createNewFile(new File(dirPath, fileName), fileContentLineList, fileSetup)
    }

    boolean createNewFile(String filePath, List fileContentLineList){
        createNewFile(filePath, fileContentLineList, new FileSetup())
    }

    boolean createNewFile(String filePath, List fileContentLineList, FileSetup fileSetup){
        createNewFile(new File(filePath), fileContentLineList, fileSetup)
    }

    boolean createNewFile(File file, List fileContentLineList){
        createNewFile(file, fileContentLineList, new FileSetup())
    }

    boolean createNewFile(File newFile, List fileContentLineList, FileSetup opt){
        write(newFile, fileContentLineList, getMergedOption(opt))
    }

    /*************************
     * 특정 디렉토리의 모든 파일들을 -> 지우기
     *************************/
    static boolean emptyDirectory(String dirPathToDelete, List excludePathList) throws Exception{
        File dirToDelete = new File(dirPathToDelete)
        dirToDelete.listFiles().each{ File fileToDelete ->
            // Delete
            if ( !isExcludeFile(fileToDelete, excludePathList) ){
                if (fileToDelete.isDirectory()){
                    rmdir(fileToDelete, excludePathList)
                }else{
                    logger.debug("Deleting ${fileToDelete.path}")
                    if (!fileToDelete.delete())
                        logger.info(" - Failed - To Delete ${fileToDelete.path}")
                }
            }
        }
        return true
    }

    /*************************
     * 특정 디렉토리을 -> 완전 지우기
     *************************/
    static boolean rmdir(final File dirToDelete, List excludePathList) {
        if (dirToDelete.isDirectory() && !isExcludeFile(dirToDelete, excludePathList) ){
            // Delete Sub
            dirToDelete.listFiles().each{ File fileToDelete ->
                // Delete File
                if ( !isExcludeFile(fileToDelete, excludePathList) ){
                    if (fileToDelete.isDirectory()){
                        rmdir(fileToDelete, excludePathList)
                    }else{
//                        logger.info("Deleting ${fileToDelete.path}")
                        logger.debug "Deleting ${fileToDelete.path}"
                        if (!fileToDelete.delete())
                            logger.info " - Failed - To Delete ${fileToDelete.path}"
                    }
                }
            }
            // Delete Empty Directory
//            logger.info("Deleting ${dirToDelete.path}")
            logger.debug "Deleting ${dirToDelete.path}"
            if (!dirToDelete.delete())
                logger.info " - Failed - To Delete ${dirToDelete.path}"
        }
        return true
    }

    /*************************
     * 특정 디렉토리의 모든 파일들을 -> 다른 특정 디렉토리로 옴기기
     *************************/
    boolean moveAllInDirectory(String beforeDirPath, String afterDirPath, List excludePathList) throws Exception{
        File beforeDir = new File(beforeDirPath)
        beforeDir.listFiles().each{ File fileToMove ->
            // Move
            if ( !isExcludeFile(fileToMove, excludePathList) ){
                File afterFile  = new File(afterDirPath, fileToMove.name)
                if (fileToMove.renameTo(afterFile)){
                    logger.debug("\n - Complete - Move ${fileToMove.path} To ${afterFile.path}")
                }
            }
        }
        return true
    }





    /******************************************************************************************
     ******************************************************************************************
     *
     * CASE STATIC
     *
     ******************************************************************************************
     ******************************************************************************************/
    /*************************
     * CHMOD
     *************************/
    static boolean chmod(File file, String chmodString){
        if (chmodString){
            if (chmodString.length() > 3)
                chmodString.substring(chmodString.length()-3, chmodString.length())
            else if (chmodString.length() < 3)
                chmodString += ((3 - chmodString.length()).collect{'4'}.join(''))
        }else{
            return false
        }

        int ownerNumber = chmodString.substring(0,1).toInteger()
        int groupNumber = chmodString.substring(1,2).toInteger()
        int otherNumber = chmodString.substring(2,3).toInteger()

        //- Clear chmod
        clearChmod(file)
        //- Group, Other
        chmod(file, groupNumber, false)
        //- Owner
        chmod(file, ownerNumber, true)
    }

    static boolean chmod(File file, int number, isOwnerOnly){
        if ([1,3,5,7].contains(number))
            file.setExecutable(true, isOwnerOnly)
        if ([2,3,6,7].contains(number))
            file.setWritable(true, isOwnerOnly)
        if ([4,5,6,7].contains(number))
            file.setReadable(true, isOwnerOnly)
    }

    static void clearChmod(File file){
        file.setReadable(false, false)
        file.setWritable(false, false)
        file.setExecutable(false, false)
    }

    /*************************
     * MKDIRS
     *************************/
    static boolean mkdirs(String path){
        boolean isOk = false
        File dir = new File(path)
        if (!dir.exists()){
            isOk = dir.mkdirs()
            logger.debug "  - Created Directory: ${dir.path}\n"
        }else{
            logger.debug "  - Directory already exists: ${dir.path}\n"
        }
        return isOk
    }

    static boolean mkdirs(String path, Map<String, Map<String, Object>> buildStructureMap, boolean modeLog){
        //Log
        if (modeLog){
            logger.debug "- Structure: ${buildStructureMap}"
            logger.debug "- Dest Path: ${path}"
        }
        //Make Directory Structure
        return mkdirs(path, buildStructureMap)
    }

    static boolean mkdirs(String path, Map<String, Map<String, Object>> buildStructureMap){
        if (!buildStructureMap)
            return mkdirs(path)

        buildStructureMap.each{ String directoryName, Map subDirectoryMap ->
            //Make Directory
            String dirPath = getFullPath("${path}/${directoryName}")
            mkdirs(dirPath)
            //Make Sub Directory
            mkdirs(dirPath, subDirectoryMap)
        }
        return true
    }

    static boolean autoMkdirs(String destPath){
        String destDirPath = getLastDirectoryPath(destPath)
        //Do AUTO MKDIR
        FileMan.mkdirs(destDirPath)
        //Check Directory
        if (!new File(destDirPath).isDirectory() && !new File(destDirPath).exists())
            throw new Exception('There is No Directory. OR Maybe, It Failed To Create Directory.')
        return true
    }

    /*************************
     * Get Relative Path
     *************************/
    static String getRelativePath(String from, String to){
//        new File(libPath).toURI().relativize(new File(installerHome).toURI())
        String relPath
        //0. Ready for diff
        List<String> fromDepthList  = getLastDirectoryPath(from).split(/[\/\\]/) - [""]
        List<String> toDepthList    = getLastDirectoryPath(to).split(/[\/\\]/) - [""]
        String fromFileName     = getLastFileName(from)
        String toFileName       = getLastFileName(to)
        String addFileName      = (fromFileName && !toFileName) ? fromFileName : (fromFileName && toFileName) ? toFileName : ''
        int sameDepthLevel = -1
        //1. Get Same Depth
        for (int i=0; i<fromDepthList.size(); i++){
            String fromDirName = fromDepthList[i]
            String toDirName = toDepthList[i]
            if (!fromDirName || !toDirName || !fromDirName.equals(toDirName)){
                break
            }else{
                sameDepthLevel = i
            }
        }
        //2. Gen Relative Dir Path
        // - Step One - CD
        int diffStartIndex  = sameDepthLevel + 1
        int fromLastIndex   = fromDepthList.size() - 1
        int toLastIndex     = toDepthList.size() - 1
        int diffDepthCount = sameDepthLevel - fromLastIndex
        if (diffDepthCount == 0){
            relPath = '.'
        }else if (diffDepthCount < 0){
            relPath = fromDepthList[diffStartIndex..fromLastIndex].collect{ '..' }.join('/')
        }
        // - Step Two - CD
        if (sameDepthLevel <= fromLastIndex && sameDepthLevel < toLastIndex){
            if (isDifferentRootDir(from, to))
                relPath = getFullPath(to)
            else
                relPath += "/${toDepthList[diffStartIndex..toLastIndex].join('/')}"


        }
        return (addFileName) ? "${relPath}/${addFileName}" : relPath
    }

    //Get Last Directory
    static String getLastDirectoryPath(String filePath){
        return (isFile(filePath)) ? new File(filePath).getParentFile().getPath() : filePath
    }

    /*************************
     * Check
     *************************/
    static boolean checkSourceFiles(String sourcePath, List filePathList){
        if (!filePathList) {
            logger.warn("Does not exist Source File. [ ${sourcePath} ]")
//            throw new IOException("Does not exist Source File, [ ${sourcePath} ]")
            return false
        }
        return true
    }

    static boolean removeFileSizeZeroEntry(String targetRootPath, List<String> entryList, boolean modeExcludeFileSizeZero){
        List<Integer> sizeZeroEntryIndexList = []
        if (modeExcludeFileSizeZero){
            //Search Size Zero File
            entryList.eachWithIndex{ String entryPath, int index ->
                File file = new File(targetRootPath, entryPath)
                if (file.exists() && file.isFile() && !file.isDirectory() && file.length() == 0){
                    sizeZeroEntryIndexList << index
                }
            }
            //Remove by index
            sizeZeroEntryIndexList.reverseEach{ Integer sizeZeroEntryIndex ->
                String sizeZeroEntryPath = entryList[sizeZeroEntryIndex]
                File file = new File(targetRootPath, sizeZeroEntryPath)
                logger.debug " (Exclude File Size Zero) ${file.path}"
                entryList.remove(sizeZeroEntryIndex)
            }
        }
        return true
    }


    static boolean checkPath(String sourcePath, String destPath){
        //Check Source Path
        if (!sourcePath)
            throw new IOException('No Source Path, Please Set Source Path.')
//        if (!sourcePath.contains('*') && !new File(sourcePath).exists())
//            throw new IOException("Does not exist Source Path, [ ${sourcePath} ]")
        if (isRootPath(sourcePath))
            throw new IOException('Source Path naver be seted rootPath on FileSystem.')
        //Check Dest Path
        if (!destPath)
            throw new IOException('No Dest Path, Please Set Dest Path.')
        if (isRootPath(destPath))
            throw new IOException('Dest Path naver be seted set rootPath on FileSystem.')
        return true
    }

    static boolean checkDir(String path, boolean modeAutoMkdir){
//        File baseDir = new File(path).getParentFile()
        File baseDir = new File(getLastDirectoryPath(path))
        if (modeAutoMkdir){
            autoMkdirs(baseDir.path)
            if (!baseDir.exists())
                throw new Exception("< Failed to CREATE Directory > Directory To Save File Could Not be Created", new Throwable("You Need To Check Permission Check And... Some... "))
        }else{
            if (!baseDir.exists())
                throw new Exception("< Failed to WRITE File> No Directory To Save File ", new Throwable("Check Please."))
        }
        return true
    }

    static boolean exists(String filePath){
        return new FileMan(filePath).exists()
    }

    static boolean checkFile(String path){
        if (new File(path).exists())
            throw new Exception("< Failed to WRITE File > File Already Exists. ${path}", new Throwable("Check Please. ${path}"))
        return true
    }

    static boolean checkFile(String path, boolean modeAutoOverWrite){
        if (!modeAutoOverWrite)
            checkFile(path)
        return true
    }

    static boolean checkFiles(List<String> entry, boolean modeAutoOverWrite){
        if (!modeAutoOverWrite){
            entry.each{ String path ->
                checkFile(new File(path).path)
            }
        }
        return true
    }

    static boolean checkFiles(String destPath, List<String> entry, boolean modeAutoOverWrite){
        destPath = getFullPath(replaceWeirdString(destPath))
        if (!modeAutoOverWrite) {
            if (isFile(destPath) || new File(destPath).isFile()) {
                checkFile(destPath)
            } else {
                String rootPath = getLastDirectoryPath(destPath)
                entry.each { String relPath ->
                    checkFile(new File(rootPath, relPath).path)
                }
            }
        }
        return true
    }

    //Check File? or Directory?
    static boolean isFile(String filePath){
        // - 끝이 구분자로 끝나면 => 폴더로 인식
        if (filePath.endsWith('/')|| filePath.endsWith('\\'))
            return false
        // - 확장자가 존재하면 => 부모를 폴더로 인식
        else if (new File(filePath).getName().contains('.'))
            return true
        // - 그외에는 무조건 폴더로 인식
        else
            return false
    }

    static boolean isRootPath(String filePath){
        try{
            filePath = replaceWeirdString(filePath)
            //Simplely Check
            if (!filePath)
                return false

            if (filePath.equals('/') || filePath.equals('\\'))
                return true

            if (filePath.length() < 2)
                return false

            //Analisys Path
            String rootName
            List fromOriginDepthList = filePath.split(/[\/\\]/) - [""]
            rootName = (fromOriginDepthList[0] as String)?.toUpperCase()

            if (fromOriginDepthList && fromOriginDepthList.size() > 1)
                return false

            //Maybe WIndows Drivers Check
            if (rootName){
                if (filePath.startsWith('/') || filePath.startsWith('\\') || rootName.indexOf(':') != -1){
                    List<File> rootList = new File('.').listRoots()
                    if (rootList.findAll { (it as String).startsWith(rootName) })
                        return true
                    else
                        return false
                }
            }

        }catch(e){
        }
        return false
    }

    // File Path -> FileName String
    // Directory Path -> Empty String
    static String getLastFileName(String filePath){
        List fromOriginDepthList = filePath.split(/[\/\\]/)
        return (isFile(filePath)) ? fromOriginDepthList[fromOriginDepthList.size()-1] : null
    }

    /*************************
     * WRITE
     *************************/
    static boolean write(String newFilePath, String content){
        return write(newFilePath, content, new FileSetup())
    }

    static boolean write(String newFilePath, String content, boolean modeAutoMkdir){
        return write(newFilePath, content, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean write(String newFilePath, String content, FileSetup opt){
        List<String> fileContentLineList = []
        content.eachLine{ fileContentLineList << it }
        return write(newFilePath, fileContentLineList, opt)
    }

    static boolean write(String newFilePath, List<String> fileContentLineList){
        return write(new File(getFullPath(newFilePath)), fileContentLineList, new FileSetup())
    }

    static boolean write(String newFilePath, List<String> fileContentLineList, FileSetup opt){
        return write(new File(getFullPath(newFilePath)), fileContentLineList, opt)
    }

    static boolean write(String newFilePath, List<String> fileContentLineList, boolean modeAutoMkdir){
        return write(new File(getFullPath(newFilePath)), fileContentLineList, modeAutoMkdir)
    }

    static boolean write(File newFile, List<String> fileContentLineList, boolean modeAutoMkdir){
        return write(newFile, fileContentLineList, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean write(File newFile, List<String> fileContentLineList, FileSetup opt){
        //Check Path Parameter
        String destPath = getFullPath(newFile.path)
        checkPath('dummy*', destPath)
        //Check Dest
        String lastDirPath = newFile.getParentFile().getPath() //name not contains dot can be file on Here
        checkDir(lastDirPath, opt.modeAutoMkdir)
        if (!opt.modeAppendWrite)
            checkFile(destPath, opt.modeAutoOverWrite)
        //Write File to Dest
        File file = new File(destPath)
        boolean alreadyExist = file.exists()
        try{
            if (opt.modeAppendWrite){
                file.withWriterAppend(opt.encoding){ out ->
                    // METHOD A. Auto LineBreak
                    if (!opt.lineBreak)
                        fileContentLineList.each{ String oneLine -> out.println oneLine }
                    // METHOD B. Custom LineBreak
                    else
                        out.print ( fileContentLineList.join(opt.lineBreak) + ((opt.lastLineBreak)?:'') )
                }
            }else{
                file.withWriter(opt.encoding){ out ->
                    // METHOD A. Auto LineBreak
                    if (!opt.lineBreak)
                        fileContentLineList.each{ String oneLine -> out.println oneLine }
                    // METHOD B. Custom LineBreak
                    else
                        out.print ( fileContentLineList.join(opt.lineBreak) + ((opt.lastLineBreak)?:'') )
                }
            }
            if (opt.chmod)
                chmod(file, opt.chmod)
            if (!alreadyExist)
                logger.debug(" - Complete - Create ${file.path} \n")
        }catch(Exception e){
            logger.error(" - Failed - To Create ${file.path} \n", e)
            throw new Exception("< Failed to WRITE File >", new Throwable("You Need To Check Permission Check And... Some... "))
        }
        return true
    }

    /*************************
     * APPEND (write)
     *************************/
    static boolean append(String newFilePath, String content){
        return append(newFilePath, content, new FileSetup())
    }

    static boolean append(String newFilePath, String content, boolean modeAutoMkdir){
        return append(newFilePath, content, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean append(String newFilePath, String content, FileSetup opt){
        List<String> fileContentLineList = []
        content.eachLine{ fileContentLineList << it }
        return append(newFilePath, fileContentLineList, opt)
    }

    static boolean append(String newFilePath, List<String> fileContentLineList, FileSetup opt){
        return append(new File(getFullPath(newFilePath)), fileContentLineList, opt)
    }

    static boolean append(String newFilePath, List<String> fileContentLineList, boolean modeAutoMkdir){
        return append(new File(getFullPath(newFilePath)), fileContentLineList, modeAutoMkdir)
    }

    static boolean append(File newFile, List<String> fileContentLineList, boolean modeAutoMkdir){
        return append(newFile, fileContentLineList, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean append(File newFile, List<String> fileContentLineList, FileSetup opt){
        opt.modeAppendWrite = true
        return write(newFile, fileContentLineList, opt)
    }


     /*************************
     * COPY
     * 파일 => 파일 (파일명변경)
     * *   => 폴더   (자동파일명)
     * 파일 => 폴더  (자동파일명)
     * 폴더 => 폴더  (자동파일명)
     *************************/
    static boolean copy(String sourcePath, String destPath){
        return copy(sourcePath, destPath, new FileSetup())
    }

    static boolean copy(String sourcePath, String destPath, boolean modeAutoMkdir){
        return copy(sourcePath, destPath, new FileSetup(modeAutoMkdir: modeAutoMkdir))
    }

    static boolean copy(String sourcePath, String destPath, FileSetup opt){
        //- Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //- Check Source
        List entryList = getEntryList(sourcePath)
        checkSourceFiles(sourcePath, entryList)
        //- Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFiles(destPath, entryList, opt.modeAutoOverWrite)
        //- Copy File to Dest
        startLogPath('COPY', sourcePath, destPath)
        if (isFile(destPath)){
            File sourceFile = new File(sourcePath)
            File destFile = new File(destPath)
            copy(sourceFile, destFile)
        }else{
            String sourceRootPath = new File(sourcePath).getParentFile().getPath()
            String destRootPath = getLastDirectoryPath(destPath)
            for (String fileRelPath : entryList){
                File sourceFile = new File(sourceRootPath, fileRelPath)
                File destFile = new File(destRootPath, fileRelPath)
                copy(sourceFile, destFile)
            }
        }
        return true
    }

    static boolean copy(File sourceFile, File destFile){
        if (sourceFile.isDirectory()){
            mkdirs(destFile.path)
        }else{
            FileChannel sourceChannel = null
            FileChannel destChannel = null
            try{
                sourceChannel = new FileInputStream(sourceFile.path).getChannel()
                destChannel = new FileOutputStream(destFile.path).getChannel()
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size())
                logger.debug "Copied: ${destFile.path}"
            }catch (Exception e){
                throw e
            }finally{
                if (sourceChannel)
                    sourceChannel.close()
                if (destChannel)
                    destChannel.close()
            }
        }
        return true
    }


    /*************************
     * MOVE
     * 파일 => 파일 (파일명변경)
     * *   => 폴더   (자동파일명)
     * 파일 => 폴더  (자동파일명)
     * 폴더 => 폴더  (자동파일명)
     *************************/
    static boolean move(String sourcePath, String destPath){
        return move(sourcePath, destPath, new FileSetup())
    }

    static boolean move(String sourcePath, String destPath, boolean modeAutoMkdir){
        return move(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean move(String sourcePath, String destPath, FileSetup opt){
        //Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //Make Entry
        List entryList = getEntryList(sourcePath)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        String destRootPath = getLastDirectoryPath(destPath)
        //Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFiles(destPath, entryList, opt.modeAutoOverWrite)
        //Log
        startLogPath('MOVE', sourcePath, destPath)
        //Move File to Dest
        try{
            new File(sourcePath).renameTo(destPath)
        }finally{
        }
        return true
    }

    /*************************
     * DELETE
     *************************/
    static boolean delete(String sourcePath){
        return delete(sourcePath, new FileSetup())
    }

    static boolean delete(String sourcePath, FileSetup opt){
        //Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        checkPath(sourcePath, 'dummy*')
        //Make Entry
        List entryList = getEntryList(sourcePath)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        //Log
        startLogPath('DELETE', sourcePath)
        //Delete File
        return delete(entryList, sourceRootPath)
    }

    static boolean delete(List entryList, String sourceRootPath){
        try{
            for (String file : entryList){
                String path = sourceRootPath + File.separator + file
                File fileToDelete = new File(path)
                logger.debug "Deleting: ${file}"
                if (fileToDelete.isFile()){
                    fileToDelete.delete()
                }else{
                    rmdir(fileToDelete, [])
                }
            }

        }catch(IOException ex){
            logger.error "<Error>"
            ex.printStackTrace()
            throw ex
        }finally{
        }
        return true
    }


    /*************************
     * COMPRESSING
     *************************/
    static boolean compress(String sourcePath){
        compress(sourcePath, new File(sourcePath).getParentFile().getPath())
    }

    static boolean compress(String sourcePath, String destPath){
        compress(sourcePath, destPath, new FileSetup())
    }

    static boolean compress(String sourcePath, String destPath, boolean modeAutoMkdir){
        compress(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean compress(String sourcePath, String destPath, FileSetup opt){
        String fileName = new File(destPath).getName()
        String extension = fileName.substring(fileName.lastIndexOf('.'))?.toLowerCase()
        if (extension){
            if (extension == 'tar')
                return tar(sourcePath, destPath, opt)
            else if (extension == 'jar')
                return jar(sourcePath, destPath, opt)
            else
                return zip(sourcePath, destPath, opt)
        }
    }


    /*************************
     * COMPRESSING - ZIP
     *************************/
    static boolean zip(String sourcePath){
        zip(sourcePath, null, new FileSetup())
    }

    static boolean zip(String sourcePath, boolean modeAutoMkdir){
        zip(sourcePath, null, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean zip(String sourcePath, FileSetup opt){
        zip(sourcePath, null, opt)
    }

    static boolean zip(String sourcePath, String destPath){
        zip(sourcePath, destPath, new FileSetup())
    }

    static boolean zip(String sourcePath, String destPath, boolean modeAutoMkdir){
        zip(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean zip(String sourcePath, String destPath, FileSetup opt){
        //- Auto DestPath
        destPath = getAutoDestPath(sourcePath, destPath, 'zip')
        //- Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //- Check Source
        List entryList = getEntryList(sourcePath)
        checkSourceFiles(sourcePath, entryList)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        String destRootPath = getLastDirectoryPath(destPath)
        removeFileSizeZeroEntry(sourceRootPath, entryList, opt.modeExcludeFileSizeZero)
        //- Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFile(destPath, opt.modeAutoOverWrite)
        //- Zip Files to Dest
        startLogPath('ZIP', sourcePath, destPath)
        return zip(entryList, sourceRootPath, destPath)
    }

    static boolean zip(List entryList, String sourceRootPath, String destPath){
        //Compress Zip
        byte[] buffer = new byte[BUFFER]
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destPath))
        try{
            for (String file : entryList){
                file = toSlash(file)
                String path = sourceRootPath + '/' + file
                logger.debug "Compressing: ${file}"
                zos.putNextEntry(new ZipEntry(file))
                if (new File(path).isFile()){
                    FileInputStream fis = new FileInputStream(path)
                    int len
                    while ((len = fis.read(buffer)) > 0){
                        zos.write(buffer, 0, len)
                    }
                    fis.close()
                }
            }

        }catch(IOException ex){
            logger.error "<Error>"
            ex.printStackTrace()
            throw ex
        }finally{
            if (zos){
                zos.closeEntry()
                zos.close()
            }
        }
        return true
    }

    /*************************
     * COMPRESSING - JAR
     *************************/
    static boolean jar(String sourcePath){
        jar(sourcePath, null, new FileSetup())
    }

    static boolean jar(String sourcePath, boolean modeAutoMkdir){
        jar(sourcePath, null, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean jar(String sourcePath, FileSetup opt){
        jar(sourcePath, null, opt)
    }

    static boolean jar(String sourcePath, String destPath){
        jar(sourcePath, destPath, new FileSetup())
    }

    static boolean jar(String sourcePath, String destPath, boolean modeAutoMkdir){
        jar(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean jar(String sourcePath, String destPath, FileSetup opt) throws IOException{
        //- Auto DestPath
        destPath = getAutoDestPath(sourcePath, destPath, 'jar')
        //- Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //- Check Source
        List entryList = getEntryList(sourcePath)
        checkSourceFiles(sourcePath, entryList)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        String destRootPath = getLastDirectoryPath(destPath)
        //- Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFile(destPath, opt.modeAutoOverWrite)
        //- Jar Files to Dest
        startLogPath('JAR', sourcePath, destPath)
        return jar(entryList, sourceRootPath, destPath)
    }

    static boolean jar(List entryList, String sourceRootPath, String destPath){
        //Compress Jar
        byte[] buffer = new byte[BUFFER]
//        JarOutputStream jos = new JarOutputStream(new FileOutputStream(destPath), manifest)
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(destPath))
        try{
            for (String file : entryList){
                String path = sourceRootPath + '/' + file
                path = path.replace("\\", "/")
                file = file.replace("\\", "/")
                logger.debug "Compressing: ${file}"
                JarEntry entry = new JarEntry(file)
                jos.putNextEntry(entry)
                if (new File(path).isFile()){
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
                    int len
                    while ( (len = bis.read(buffer)) > 0){
                        jos.write(buffer, 0, len)
                    }
                    bis.close()
                }
            }

        }catch(IOException ex){
            logger.error "<Error>"
            ex.printStackTrace()
            throw ex
        }finally{
            if (jos){
                jos.closeEntry()
                jos.close()
            }
        }
        return true
    }

    /*************************
     * COMPRESSING - TAR.GZ
     *************************/
    static boolean tar(String sourcePath){
        tar(sourcePath, null, new FileSetup())
    }

    static boolean tar(String sourcePath, boolean modeAutoMkdir){
        tar(sourcePath, null, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean tar(String sourcePath, FileSetup opt){
        tar(sourcePath, null, opt)
    }

    static boolean tar(String sourcePath, String destPath){
        tar(sourcePath, destPath, new FileSetup())
    }

    static boolean tar(String sourcePath, String destPath, boolean modeAutoMkdir){
        tar(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean tar(String sourcePath, String destPath, FileSetup opt) throws IOException{
        //- Auto DestPath
        destPath = getAutoDestPath(sourcePath, destPath, 'tar')
        //- Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //- Check Source
        List entryList = getEntryList(sourcePath)
        checkSourceFiles(sourcePath, entryList)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        String destRootPath = getLastDirectoryPath(destPath)
        //- Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFile(destPath, opt.modeAutoOverWrite)
        //- Tar Files to Dest
        startLogPath('TAR', sourcePath, destPath)
        return tar(entryList, sourceRootPath, destPath)
    }

    static boolean tar(List entryList, String sourceRootPath, String destPath){
        //Compress Tar
        byte[] buffer = new byte[BUFFER]
//        TarArchiveOutputStream taos = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(destPath)))
        TarArchiveOutputStream taos = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(destPath))))
        // TAR has an 8 gig file limit by default, this gets around that
        taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR) // to get past the 8 gig limit
        // TAR originally didn't support long file names, so enable the support for it
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
        try{
            for (String file : entryList){
                String path = sourceRootPath + '/' + file
                path = path.replace("\\", "/")
                file = file.replace("\\", "/")
                logger.debug "Compressing: ${file}"
                TarArchiveEntry entry = new TarArchiveEntry(new File(file), file)
                if (new File(path).isFile()) {
                    entry.setSize(new File(path).length())
                    taos.putArchiveEntry(entry)
                    /////A
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path))
                    int len
                    while ( (len = bis.read(buffer)) > 0 ) {
                        taos.write(buffer, 0, len)
                    }
                    bis.close()
                    /////B
//                    FileInputStream fis = new FileInputStream(path)
//                    IOUtils.copy(fis, taos)
                    taos.flush()
                }else{
                    taos.putArchiveEntry(entry)
                }
                taos.closeArchiveEntry()
            }

        }catch(IOException ex){
            logger.error "<Error>"
            ex.printStackTrace()
            throw ex
        }finally{
            if (taos){
                taos.close()
            }
        }
        return true
    }




    /*************************
     * EXTRACTING
     *************************/
    static boolean extract(String sourcePath){
        extract(sourcePath, getLastDirectoryPath(sourcePath), new FileSetup())
    }

    static boolean extract(String sourcePath, String destPath){
        extract(sourcePath, destPath, new FileSetup())
    }

    static boolean extract(String sourcePath, String destPath, boolean modeAutoMkdir){
        extract(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean extract(String filePath, String destPath, FileSetup opt){
        String fileName = new File(filePath).getName()
        String extension = fileName.substring(fileName.lastIndexOf('.'))?.toLowerCase()
        if (extension){
            if (extension == 'tar')
                return untar(filePath, destPath, opt)
            if (extension == 'jar')
                return unjar(filePath, destPath, opt)
            else
                return unzip(filePath, destPath, opt)
        }
    }

    /*************************
     * EXTRACTING - UNTAR
     *************************/
    static boolean untar(String sourcePath){
        untar(sourcePath, getLastDirectoryPath(sourcePath))
    }

    static boolean untar(String sourcePath, String destPath){
        untar(sourcePath, destPath, new FileSetup())
    }

    static boolean untar(String sourcePath, String destPath, boolean modeAutoMkdir){
        untar(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean untar(String sourcePath, String destPath, FileSetup opt){
        //- Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //- Check Source
        List<String> filePathList = getSubFilePathList(sourcePath)
        checkSourceFiles(sourcePath, filePathList)
        List<String> entryList = genEntryListFromTarFile(sourcePath)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        String destRootPath = getLastDirectoryPath(destPath)
        //- Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFiles(destPath, entryList, opt.modeAutoOverWrite)
        //- Tar File To Dest
        startLogPath('UNTAR', sourcePath, destPath)
        filePathList.each{ String sourceFilePath ->
            byte[] buffer = new byte[BUFFER]
            try{
                /** Ready **/
                FileInputStream fin = new FileInputStream(sourceFilePath)
                BufferedInputStream bis = new BufferedInputStream(fin)
                GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bis)
                TarArchiveInputStream tais = new TarArchiveInputStream(gzIn)
                TarArchiveEntry entry
                /** Read the tar entries using the getNextEntry method **/
                while ( (entry = (TarArchiveEntry) tais.getNextEntry()) != null ) {
                    File file = new File(destPath, entry.getName())
                    logger.debug "Extracting: ${file.getAbsolutePath()}"
                    if (entry.isDirectory()) {
                        file.mkdirs()
                    } else {
                        file.parentFile.mkdirs()
                        FileOutputStream fos = new FileOutputStream(file)
                        BufferedOutputStream destOs = new BufferedOutputStream(fos, BUFFER)
                        int len
                        while ( (len = tais.read(buffer, 0, BUFFER) ) != -1) {
                            destOs.write(buffer, 0, len)
                        }
                        destOs.close()
                    }
                }
                /** Close the input stream **/
                tais.close()

            }catch(IOException ex){
                ex.printStackTrace()
                throw ex
            }
        }
        return true
    }

    /*************************
     * EXTRACTING - UNZIP
     *************************/
    static boolean unzip(String sourcePath){
        unzip(sourcePath, getLastDirectoryPath(sourcePath))
    }

    static boolean unzip(String sourcePath, String destPath){
        unzip(sourcePath, destPath, new FileSetup())
    }

    static boolean unzip(String sourcePath, String destPath, boolean modeAutoMkdir){
        unzip(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean unzip(String sourcePath, String destPath, FileSetup opt){
        //- Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //- Check Source
        List<String> filePathList = getSubFilePathList(sourcePath)
        checkSourceFiles(sourcePath, filePathList)
        List<String> entryList = genEntryListFromZipFile(sourcePath)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        String destRootPath = getLastDirectoryPath(destPath)
        //- Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFiles(destPath, entryList, opt.modeAutoOverWrite)
        //- Zip File To Dest
        startLogPath('UNZIP', sourcePath, destPath)
        filePathList.each{ String sourceFilePath ->
            byte[] buffer = new byte[BUFFER]
            try{
                /** Ready **/
                ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFilePath))
                ZipEntry entry
                /** Read the zip entries using the getNextEntry method **/
                while ( (entry = zis.getNextEntry()) != null ){
                    File file = new File(destPath, entry.getName())
                    logger.debug "Extracting: ${file.getAbsolutePath()}"
                    if (entry.isDirectory()){
                        file.mkdirs()
                    }else{
                        file.parentFile.mkdirs()
                        FileOutputStream fos = new FileOutputStream(file)
                        int len
                        while ( (len = zis.read(buffer)) > 0 ) {
                            fos.write(buffer, 0, len)
                        }
                        fos.close()
                    }
                }
                /** Close the input stream **/
                zis.closeEntry()
                zis.close()

            }catch(IOException ex){
                ex.printStackTrace()
                throw ex
            }
        }
        return true
    }

    /*************************
     * EXTRACTING - UNJAR
     *************************/
    static boolean unjar(String sourcePath){
        unjar(sourcePath, getLastDirectoryPath(sourcePath))
    }

    static boolean unjar(String sourcePath, String destPath){
        unjar(sourcePath, destPath, new FileSetup())
    }

    static boolean unjar(String sourcePath, String destPath, boolean modeAutoMkdir){
        unjar(sourcePath, destPath, new FileSetup(modeAutoMkdir:modeAutoMkdir))
    }

    static boolean unjar(String sourcePath, String destPath, FileSetup opt){
        //- Check Path Parameter
        sourcePath = getFullPath(sourcePath)
        destPath = getFullPath(destPath)
        checkPath(sourcePath, destPath)
        //- Check Source
        List<String> filePathList = getSubFilePathList(sourcePath)
        checkSourceFiles(sourcePath, filePathList)
        List<String> entryList = genEntryListFromJarFile(sourcePath)
        String sourceRootPath = new File(sourcePath).getParentFile().getPath()
        String destRootPath = getLastDirectoryPath(destPath)
        //- Check Dest
        checkDir(destPath, opt.modeAutoMkdir)
        checkFiles(destPath, entryList, opt.modeAutoOverWrite)
        //- Jar File To Dest
        startLogPath('UNJAR', sourcePath, destPath)
        filePathList.each{ String sourceFilePath ->
            byte[] buffer = new byte[BUFFER]
            try{
                /** Ready **/
                JarFile jar = new JarFile(sourceFilePath)
                Enumeration enumEntries = jar.entries()
                JarEntry entry
                /** Read the jar entries using the nextElement method **/
                while (enumEntries.hasMoreElements()) {
                    entry = (JarEntry) enumEntries.nextElement()
                    File file = new File(destPath, entry.getName())
                    logger.debug "Extracting: ${file.getAbsolutePath()}"
                    if (entry.isDirectory()) {
                        file.mkdirs()
                    } else {
                        file.parentFile.mkdirs()
                        InputStream is = jar.getInputStream(entry)
                        FileOutputStream fos = new FileOutputStream(file)
                        int len
                        while ( (len = is.read(buffer)) > 0 ) {
                            fos.write(buffer, 0, len)
                        }
                        fos.close()
                        is.close()
                    }
                }
                /** Close the input stream **/
                jar.close()

            }catch(IOException ex){
                ex.printStackTrace()
                throw ex
            }
        }
        return true
    }


    static void startLogPath(String title, String sourcePath){
//        logger.debug "${title}"
        logger.debug " - Source Path: ${sourcePath}"
    }

    static void startLogPath(String title, String sourcePath, String destPath){
//        logger.debug "${title}"
        logger.debug " - Source Path: ${sourcePath}"
        logger.debug " -   Dest Path: ${destPath}"
    }

    static String getAutoDestPath(String sourcePath, String destPath, String extension){
        if (sourcePath && !destPath){
            String parentPath = new File(sourcePath).getParentFile().path
            String parentName = new File(sourcePath).getParentFile().name
            String fileName = new File(sourcePath).name.split('[.]')[0]
            destPath = sourcePath.contains('*') ? "${parentPath}/${parentName}.${extension}": "${parentPath}/${fileName}.${extension}"
        }else if (sourcePath && destPath && !isFile(destPath)){
            String parentName = new File(sourcePath).getParentFile().name
            String fileName = new File(sourcePath).name.split('[.]')[0]
            destPath = sourcePath.contains('*') ? "${destPath}/${parentName}.${extension}": "${destPath}/${fileName}.${extension}"
        }
        return destPath
    }

    /*************************
     * EntryList
     *************************/
    static List<String> getEntryList(String sourcePath){
        List<String> newEntryList = []
        sourcePath = new File(sourcePath).getPath()
        //- Get Entry's Root Path Length
        int entryRootStartIndex = 0
        String rootPath = ''
        String ParentPath = ''
        if (!isRootPath(sourcePath)){
            rootPath = new File(sourcePath).getParentFile().getPath()
            ParentPath = new File(sourcePath).getParentFile().getPath()
            if (isRootPath(ParentPath)){
                entryRootStartIndex = rootPath.length()
            }else{
                entryRootStartIndex = rootPath.length() +1
            }
        }
        //- Get Entries
        List<String> filePathList = getSubFilePathList(sourcePath)
        filePathList.each{ String onePath ->
            if (isMatchedPath(onePath, sourcePath))
                newEntryList = getEntryList(newEntryList, entryRootStartIndex, onePath)
        }
        return newEntryList
    }

    static List<String> getEntryList(List<String> entryList, int entryRootStartIndex, String filePath){
        File node = new File(filePath)
        String oneFilePath = node.getPath().substring(entryRootStartIndex, filePath.length())
        if(node.isDirectory()){
            String[] subNote = node.list()
            entryList.add(oneFilePath + System.getProperty('file.separator'))
            for (String filename : subNote){
                getEntryList(entryList, entryRootStartIndex, new File(node, filename).getPath())
            }
        }else{
            entryList.add(oneFilePath)
        }
        return entryList
    }

    /*************************
     * EntryList from Zip
     *************************/
    static List<String> genEntryListFromZipFile(String sourcePath){
        List<String> newEntryList = []
        sourcePath = getFullPath(replaceWeirdString(sourcePath))
        String rootPath = new File(sourcePath).getParentFile().getPath()
        List<String> filePathList = getSubFilePathList(sourcePath)
        filePathList.each{ String onePath ->
            if (isMatchedPath(onePath, sourcePath))
                newEntryList = genEntryListFromZipFile(newEntryList, rootPath, onePath)
        }
        return newEntryList
    }

    static List<String> genEntryListFromZipFile(List<String> entryList, String rootPath, String filePath){
        try{
            /** Ready **/
            ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(filePath)))
            ZipEntry entry
            /** Read the zip entries using the getNextEntry method **/
            while ( (entry = zis.getNextEntry()) != null ){
                entryList << entry.getName()
            }
            /** Close the input stream **/
            zis.closeEntry()
            zis.close()

        }catch(IOException ex){
            ex.printStackTrace()
            throw ex
        }
        return entryList
    }

    /*************************
     * EntryList from Jar
     *************************/
    static List<String> genEntryListFromJarFile(String sourcePath){
        List<String> newEntryList = []
        sourcePath = getFullPath(replaceWeirdString(sourcePath))
        String rootPath = new File(sourcePath).getParentFile().getPath()
        List<String> filePathList = getSubFilePathList(sourcePath)
        filePathList.each{ String onePath ->
            if (isMatchedPath(onePath, sourcePath))
                newEntryList = genEntryListFromJarFile(newEntryList, rootPath, onePath)
        }
        return newEntryList
    }

    static List<String> genEntryListFromJarFile(List<String> entryList, String rootPath, String filePath){
        try{
            /** Ready **/
            JarFile jar = new JarFile(new File(filePath))
            Enumeration enumEntries = jar.entries()
            JarEntry entry
            /** Read the jar entries using the nextElement method **/
            while (enumEntries.hasMoreElements()){
                entry = (JarEntry) enumEntries.nextElement()
                entryList << entry.getName()
            }
            /** Close the input stream **/
            jar.close()

        }catch(IOException ex){
            ex.printStackTrace()
            throw ex
        }
        return entryList
    }

    /*************************
     * EntryList from Tar
     *************************/
    static List<String> genEntryListFromTarFile(String sourcePath){
        List<String> newEntryList = []
        sourcePath = getFullPath(replaceWeirdString(sourcePath))
        String rootPath = new File(sourcePath).getParentFile().getPath()
        List<String> filePathList = getSubFilePathList(sourcePath)
        filePathList.each{ String onePath ->
            if (isMatchedPath(onePath, sourcePath))
                newEntryList = genEntryListFromTarFile(newEntryList, rootPath, onePath)
        }
        return newEntryList
    }

    static List<String> genEntryListFromTarFile(List<String> entryList, String rootPath, String filePath){
        try{
            /** Ready **/
            FileInputStream fin = new FileInputStream(new File(filePath))
            BufferedInputStream bis = new BufferedInputStream(fin)
            GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bis)
            TarArchiveInputStream tais = new TarArchiveInputStream(gzIn)
            TarArchiveEntry entry
            /** Read the tar entries using the getNextEntry method **/
            while ( (entry = (TarArchiveEntry) tais.getNextEntry()) != null ) {
                entryList << entry.getName()
            }
            /** Close the input stream **/
            tais.close()

        }catch(IOException ex){
            ex.printStackTrace()
            throw ex
        }
        return entryList
    }



    /*************************
     * Find File
     *************************/
    static File find(String fileName){
        return find([fileName])
    }

    static File find(List<String> searchFileNameList){
        return find('', searchFileNameList)
    }

    static File find(String rootPath, String fileName){
        return find(rootPath, [fileName])
    }

    static File find(String rootPath, String fileName, List<String> extensionList){
        List fileNameList = extensionList.collect{ String extension -> "${fileName}.${extension}" }
        return find(rootPath, fileNameList)
    }

    static File find(String rootPath, List<String> searchFileNameList){
        File foundFile
        for (String fileName : searchFileNameList){
            String filePath = getFullPath(rootPath, fileName)
            File file = new File(filePath)
            if (file.exists()){
                foundFile = file
                break
            }
        }
        return foundFile
    }



    /*************************
     * Find File - from this Application(.jar)
     *************************/
    static File findFromApp(String fileName){
        return findFromApp([fileName])
    }

    static File findFromApp(List<String> fileNameList){
        String rootPath = new FileMan().getThisAppFile()?.getParentFile()?.getPath()
        return find(rootPath, fileNameList)
    }



    /*************************
     * Find Resource
     *************************/
    static File findResource(String fileName){
        return findResource([fileName])
    }

    static File findResource(List<String> searchFileNameList){
        return findResource('', searchFileNameList)
    }

    static File findResource(String rootPath, String fileName){
        return findResource(rootPath, [fileName])
    }

    static File findResource(String rootPath, String fileName, List<String> extensionList){
        List fileNameList = extensionList.collect{ String extension -> "${fileName}.${extension}" }
        return findResource(rootPath, fileNameList)
    }

    static File findResource(String rootPath, List<String> searchFileNameList){
        File foundFile
        for (String fileName : searchFileNameList){
            File file = getFileFromResource( (rootPath) ? "${rootPath}/${fileName}" : fileName )
            if (file && file.exists()){
                foundFile = file
                break
            }
        }
        return foundFile
    }

    /*************************
     * Find All File
     *************************/
    static List<File> findAll(String searchFilePath){
        String rootPath = ''
        String fullPath = getFullPath(searchFilePath)

        int firstAsterisk = fullPath.indexOf('*')
        if (firstAsterisk != -1){
            int slashIndexBeforeFirstAsterisk = (firstAsterisk != -1) ? fullPath.lastIndexOf('/', firstAsterisk) : -1
            if (slashIndexBeforeFirstAsterisk > 0){
                //- from specific path
                rootPath = fullPath.substring(0, slashIndexBeforeFirstAsterisk +1)
            }else if (slashIndexBeforeFirstAsterisk == 0){
                //- from root
                rootPath = '/'
            }else{
                //- from now
                rootPath = './'
            }
        }else{
            File foundFile = find(searchFilePath)
            return (foundFile) ? [foundFile] : []
        }
        return findAll(rootPath, searchFilePath, null)
    }

    static List<File> findAll(String rootPath, String searchFileName){
        return findAll(rootPath, searchFileName, null)
    }

    static List<File> findAll(String rootPath, String searchFileName, Closure eachFoundFileClosure){
        return findAll(rootPath, searchFileName, null, eachFoundFileClosure)
    }

    static List<File> findAll(String rootPath, String searchFileName, def condition){
        return findAll(rootPath, searchFileName, condition, null)
    }

    static List<File> findAll(String rootPath, String searchFileName, def condition, Closure eachFoundFileClosure){
        List<File> newEntryList = []
        List<File> filePathList = getSubFilePathList(rootPath)
        String matchPattern = getMatcherPattern(rootPath, searchFileName)

        int numberOfAsterisk = searchFileName.count("*")

        //- Target: One Level
        if (numberOfAsterisk <= 1){
            newEntryList = getSubFilePathList(searchFileName)
            return newEntryList.collect{ new File(it) }
        }

        //- Target: Multiple Level
        // Find File
        filePathList.eachWithIndex{ String onePath, int i ->
            newEntryList = findAll(newEntryList, onePath, matchPattern) { File foundFile ->
                String foundFilePath = foundFile.getPath()
                Map result = [:]
                result[new File(foundFilePath).getName()] = true
                def foundObject = Util.find(result, condition) { dataObject, conditionObject ->
                    //파일 존재 검사후, 조건값과 비교
                    Map matchedObject = conditionObject.findAll { path, existCondition ->
                        boolean isExist = new File(getLastDirectoryPath(foundFilePath), path).exists()
                        return (isExist == existCondition)
                    }
                    //모두 일치하면 True
                    return (matchedObject.size() == conditionObject.size())
                }

                //조건 일치시
                if (foundObject) {
                    return (eachFoundFileClosure) ? eachFoundFileClosure(foundFile) : true
                } else {
                    return false
                }
            }
        }
        return newEntryList
    }

    /*************************
     * Find File - with Condition, progressBar
     * 1. You can get parameter made by type of Map, When you use closure.
     *
     * 2. If you wanna print some, then you can add Some String to data.stringList on the closure.
     * - ex)
     *      data.stringList.add("String you wanna say")
     *************************/
    static List<File> findAllWithProgressBar(String rootPath, String searchFileName){
        return findAllWithProgressBar(rootPath, searchFileName, null, null)
    }

    static List<File> findAllWithProgressBar(String rootPath, String searchFileName, Closure closure){
        return findAllWithProgressBar(rootPath, searchFileName, null, closure)
    }

    static List<File> findAllWithProgressBar(String rootPath, String searchFileName, def condition){
        return findAllWithProgressBar(rootPath, searchFileName, condition, null)
    }

    static List<File> findAllWithProgressBar(String rootPath, String searchFileName, def condition, Closure eachFoundFileClosure){
        List<File> newEntryList = []
        rootPath = (rootPath) ? rootPath : ''
        List<File> filePathList = getSubFilePathList(rootPath+'/*')
        String matchPattern = getMatcherPattern(rootPath, searchFileName)

        logger.debug('=============== Checking before find files')
        logger.trace('----- Root Files')
        logger.debug(filePathList.toListString())

        if (!filePathList)
            throw new Exception("There are no Directories or Files [${rootPath}]")

        int barSize = 20
        int count = 0

        // Find File
        Util.eachWithTimeProgressBar(filePathList, barSize){ data ->

            String onePath = data.item

            newEntryList = findAll(newEntryList, onePath, matchPattern){ File foundFile ->
                String foundFilePath = foundFile.getPath()
                Map result = [:]

                String fileName = new File(foundFilePath).getName()
                result[fileName] = true
                def foundObject = Util.find(result, condition){ dataObject, conditionObject ->
                    //파일 존재 검사후, 조건값과 비교
                    Map matchedObject = conditionObject.findAll{ path, existCondition ->
                        boolean isExist = new File(getLastDirectoryPath(foundFilePath), path).exists()
                        return (isExist == existCondition)
                    }
                    //모두 일치하면 True
                    return (matchedObject.size() == conditionObject.size())
                }

                //조건 일치시
                if (foundObject){
                    Map foundData = [item:foundFile, count:++count, stringList:data.stringList]
                    return (eachFoundFileClosure) ? eachFoundFileClosure(foundData) : true
                }else{
                    return false
                }
            }
        }
        return newEntryList
    }

    /*************************
     * Find File (Core - Recursively)
     *************************/
    static List<File> findAll(List<File> entryList, String filePath, String searchPattern){
        return findAll(entryList, filePath, searchPattern, null)
    }

    static List<File> findAll(List<File> entryList, String filePath, String searchPattern, Closure eachFoundFileClosure){
        File node = new File(filePath)
        boolean pathIsMatched = isMatchedPath(filePath, searchPattern)
        if (pathIsMatched){
            if (eachFoundFileClosure){
                if (eachFoundFileClosure(node)){
                    entryList.add(node)
                }
            }else{
                entryList.add(node)
            }
        }

        if(node.isDirectory()){
            String[] subNodes = node.list()
            for (String filename : subNodes){
                findAll(entryList, new File(node, filename).getPath(), searchPattern, eachFoundFileClosure)
            }
        }
        return entryList
    }




    static String getMatcherPattern(String filePath, String searchFileName){
        String matchPattern = null
        String lastDirectoryPath = getLastDirectoryPath(filePath)
        matchPattern = getFullPath(lastDirectoryPath, searchFileName)
        return matchPattern
    }

    static boolean isMatchedPath(String onePath, String rangePath){
        String regexpStr = toSlash(rangePath)
                                    .replace('(', '\\(').replace(')', '\\)')
                                    .replace('[', '\\[').replace(']', '\\]')
                                    .replace('.', '\\.').replace('$', '\\$')
                                    .replace('*',"[^\\/\\\\]*")
                                    .replace('[^\\/\\\\]*[^\\/\\\\]*/','(\\S*[\\/\\\\]|)')
                                    .replace('[^\\/\\\\]*[^\\/\\\\]*',"\\S*")
        return onePath.replace('\\', '/').matches(regexpStr)
    }

    static List<String> getSubFilePathList(String filePath){
        return getSubFilePathList(filePath, '')
    }

    static List<String> getSubFilePathList(String filePath, String extension){
        def filePathList = []

        //Root of FileSystem => 정확한 루트를 따진다. (Windows Drive...)
        List<File> rootList = new File('/').listRoots()
        boolean isWindows = (rootList && rootList.size() > 1)
        if (isRootPath(filePath)){
            /** Maybe on Windows **/
            if (isWindows){
                if (filePath == '/' || filePath == '\\'){
                    filePathList = [new File('/').path]
                }else{
                    String rootKeyString = filePath.replaceAll('\\W*', '')?.toUpperCase()
                    String rootName = rootList.find { it.toString().contains(rootKeyString) }
                    filePathList = rootName ? [new File(filePath).path] : []
                }

            /** Maybe on Others(Unix & Linux) **/
            }else{
                filePath = filePath.contains('*') ? filePath : "${filePath}"
            }

        }else{
            if (isWindows && filePath == '/*'){
                rootList.each{ File root ->
                    String rootName = root.toString()
                    filePathList << rootName
                }
            }else{
                String fullPath = getFullPath(filePath)
//                File file = new File(fullPath)
                // check files (new)
                if (!fullPath){
                }else if (fullPath.contains('*')){
                    File parentFile = new File(fullPath).getParentFile()
                    File[] parentFileList = parentFile.listFiles()
                    parentFileList.each{ File f ->
                        if (isMatchedPath(f.path, fullPath))
                            filePathList << f.path
                    }
                }else{
                    filePathList << new File(fullPath).path
                }
                // check extension
                if (extension){
                    filePathList = filePathList.findAll{
                        int lastDotIdx = it.lastIndexOf('.')
                        String itExtension = it.substring(lastDotIdx+1).toUpperCase()
                        String acceptExtension = extension.toUpperCase()
                        return ( itExtension.equals(acceptExtension) )
                    }
                }
            }
        }
        return filePathList
    }

    static File getFileFromResource(String resourcePath){
        return new FileMan().readResource(resourcePath).getSourceFile()
    }

    static File getFileFromResourceWithoutException(String resourcePath){
        File file = null
        try{
            file = getFileFromResource(resourcePath)
        }catch(e){
            //Ignore
        }
        return file
    }

    static List<String> getListFromFile(String filePath){
        return new FileMan(filePath).getLineList()
    }

    static List<String> getListFromFile(File file){
        return new FileMan(file).getLineList()
    }

    static String getStringFromFile(String filePath){
        return new FileMan(filePath).read().getContent()
    }

    static String getStringFromFile(File file){
        return new FileMan(file).read().getContent()
    }




    /******************************************************************************************
     ******************************************************************************************
     *
     * CASE INSTANCE
     *
     ******************************************************************************************
     ******************************************************************************************/
    FileMan setSource(String filePath){
        filePath = getFullPath(filePath)
        return setSource(new File(filePath))
    }
    FileMan setSource(File file){
        this.sourceFile = file
        return this
    }

    boolean exists(){
        return sourceFile.exists()
    }

    /*************************
     * backup
     *************************/
    FileMan backup(){
        return backup(globalOption)
    }
    FileMan backup(FileSetup opt){
        opt = getMergedOption(opt)
        if (opt.modeAutoBackup){
            String filePath = sourceFile.getPath()
            String backupPath = (opt.backupPath) ?: "${filePath}.bak_${new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())}"
            return backup(backupPath)
        }else{
            return this
        }
    }

    FileMan backup(String destPath){
        String originPath = destPath
        int maxTryCount = 5
        (0..maxTryCount).any{ int tryIndex ->
            try{
                if (tryIndex)
                    destPath = "${originPath}_${String.valueOf(tryIndex)}"
                copy(destPath)
                return true
            }catch (e){
                if (0 <= tryIndex && tryIndex < maxTryCount)
                    logger.debug("Try to backup one more... (Count: ${tryIndex +1})")
                else if (tryIndex == maxTryCount)
                    throw e
                return false
            }
        }
        return this
    }


    /*************************
     * copy
     *************************/
    FileMan copy(String destPath){
        return copy(destPath, globalOption)
    }

    FileMan copy(String destPath, FileSetup opt){
        opt = getMergedOption(opt)
        copy(sourceFile.path, destPath, opt)
        return this
    }


    /*************************
     * move
     *************************/
    FileMan move(String destPath){
        return move(destPath, globalOption)
    }

    FileMan move(String destPath, FileSetup opt){
        opt = getMergedOption(opt)
        move(sourceFile.path, destPath, opt)
        return this
    }

    /*************************
     * read
     *************************/
    FileMan read(){
        return read(sourceFile, globalOption)
    }

    FileMan read(FileSetup fileSetup){
        return read(sourceFile, fileSetup)
    }

    FileMan read(Long fromLineNumber){
        return read(sourceFile, fromLineNumber, null, globalOption)
    }

    FileMan read(Long fromLineNumber, FileSetup fileSetup){
        return read(sourceFile, fromLineNumber, null, fileSetup)
    }

    FileMan read(Long fromLineNumber, Long lineCount){
        return read(sourceFile, fromLineNumber, lineCount, globalOption)
    }

    FileMan read(Long fromLineNumber, Long lineCount, FileSetup fileSetup){
        return read(sourceFile, fromLineNumber, lineCount, fileSetup)
    }


    FileMan read(File file){
        return read(file, globalOption)
    }

    FileMan read(File file, FileSetup fileSetup){
        List<String> lineList = getLineList(file, fileSetup)
        read(lineList)
        return this
    }

    FileMan read(File file, Long fromLineNumber, Long lineCount, FileSetup fileSetup){
        List<String> lineList = getLineList(file, fromLineNumber, lineCount, fileSetup)
        read(lineList)
        return this
    }



    FileMan readResource(String resourcePath){
        String name = "/${resourcePath}"
//        File resourceFile = getFileFromResource(resourcePath)

        //Works in IDE
//        URL url = getClass().getResource(absolutePath);
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath)
        int fileNameLastDotIndex = resourcePath.lastIndexOf('.')
        String fileNameExtension = (fileNameLastDotIndex != -1) ? resourcePath.substring(fileNameLastDotIndex +1)?.toLowerCase() : ''
        File file
        if (!url){
            file = null
        }else if (url.toString().startsWith("jar:")){
            //Works in JAR
            try {
                InputStream input = getClass().getResourceAsStream(name)
                input = input != null ? input : Thread.currentThread().getContextClassLoader().getResourceAsStream(name)
                input = input != null ? input : getClass().getResourceAsStream(resourcePath)
                if (getClass().getClassLoader()){
                    input = input != null ? input : getClass().getClassLoader().getResourceAsStream(resourcePath)
                }

                if (input == null)
                    throw new Exception("Does not exists file from resource [${resourcePath}]")

                file = File.createTempFile("tempfile", ".${fileNameExtension}")
                OutputStream out = new FileOutputStream(file)
                int len
                byte[] bytes = new byte[1024]
                while ((len = input.read(bytes)) != -1) {
                    out.write(bytes, 0, len)
                }
                file.deleteOnExit()
            } catch (IOException ex) {
                ex.printStackTrace()
            }
        }else{
            //Works in your IDE, but not from a JAR
            file = new File(url.getFile())
        }
        if (file != null && !file.exists())
            throw new FileNotFoundException("Error: File " + file + " not found!")

        File resourceFile = file

        if (resourceFile == null)
            throw new Exception("Does not exists file from resource. [${resourcePath}]")

        sourceFile = file
        return read(resourceFile, globalOption)
    }

    FileMan readFile(String filePath){
        filePath = getFullPath(filePath)
        return read(filePath, globalOption)
    }

    FileMan read(String text){
        originalContent = text
        content = "${originalContent}"
        return this
    }

    FileMan read(List lineList){
        originalContent = lineList.join(System.getProperty("line.separator"))
        content = "${originalContent}"
        return this
    }

    /*************************
     * write
     *************************/
    FileMan write(){
        return write(sourceFile, globalOption)
    }

    FileMan write(FileSetup fileSetup){
        return write(sourceFile, fileSetup)
    }

    FileMan write(String filePath){
        return write(filePath, globalOption)
    }

    FileMan write(String filePath, FileSetup fileSetup){
        filePath = getFullPath(filePath)
        return write(new File(filePath), fileSetup)
    }

    FileMan write(File fileToWrite, FileSetup fileSetup){
        List<String> lineList = []
        content.eachLine{ lineList << it }
        createNewFile(fileToWrite, lineList, fileSetup)
        return this
    }



    def analysis(){
        return ""
    }

    def report(){
        return ""
    }

    /*************************
     * Changed
     *************************/
    static boolean isChanged(String filePathA, String filePathB){
        //Analisys Directories
        File fileA = new File(filePathA)
        File fileB = new File(filePathB)
        if (fileA.isDirectory() || fileB.isDirectory()){
            return !(fileA.isDirectory() && fileB.isDirectory())
        }

        //Analisys Files
        long start = System.nanoTime();
        logger.trace("  - Comparing: ${filePathA}  and  ${filePathB}");
        FileChannel chA = new RandomAccessFile(filePathA, "r").getChannel();
        FileChannel chB = new RandomAccessFile(filePathB, "r").getChannel();
        if (chA.size() != chB.size()) {
            logger.trace("    !! Files have different length.")
            return true
        }
        long size = chA.size();
        ByteBuffer mA = chA.map(FileChannel.MapMode.READ_ONLY, 0L, size);
        ByteBuffer mB = chB.map(FileChannel.MapMode.READ_ONLY, 0L, size);
        for (int pos = 0; pos < size; pos++) {
            if (mA.get(pos) != mB.get(pos)) {
                logger.trace("   !! Files differ at position " + pos)
                return true
            }
        }
        logger.trace("   !! Files are identical.");
        long end = System.nanoTime();
        logger.trace(" - Execution time: " + (end - start) / 1000000 + "ms");
        return false
    }

    /*************************
     * replace
     *************************/
    FileMan replace(Map replaceMap){
        if (replaceMap)
            logger.debug "Replacement) ${replaceMap.toMapString()}"
        replaceMap.each{ String target, String replacement ->
            replace(target, replacement)
        }
        return this
    }
    FileMan replace(String target, String replacement){
        replacement = getRightReplacement(replacement)
        //LOG
        logger.debug "from) ${target}"
        logger.debug "  to) ${replacement}"
        //REPLACE
        content = content.replaceAll(target, replacement)
        return this
    }

    /*************************
     * replace line
     *************************/
    FileMan replaceLine(Map replaceLineMap){
        if (replaceLineMap)
            logger.debug "Line Replacement) ${replaceLineMap.toMapString()}"
        replaceLineMap.each{ String target, String replacement ->
            replaceLine(target, replacement)
        }
        return this
    }
    FileMan replaceLine(String target, String replacement){
        String targetPattern = target
                .replace('(', '\\(').replace(')', '\\)')
                .replace('[', '\\[').replace(']', '\\]')
                .replace('.','\\.').replace('$','\\$').replace('#','\\#')
        String patternToGetProperty = ".*" + targetPattern + ".*"
        Matcher matchedList = Pattern.compile(patternToGetProperty, Pattern.MULTILINE).matcher(content)
        replacement = getRightReplacement(replacement)
        //LOG
        if (matchedList.size()){
            matchedList.eachWithIndex{ String from, int i ->
                logger.debug "from) ${from}"
                logger.debug "  to) ${replacement}"
            }
        }
        //REPLACE
        content = matchedList.replaceAll(replacement)
        return this
    }

    /*************************
     * replace property
     *************************/
    FileMan replaceProperties(Map replacePropertyMap){
        if (replacePropertyMap)
            logger.debug "Property Replacement: ${replacePropertyMap.toMapString()}"
        replacePropertyMap.each{ String target, String replacement ->
            replaceProperties(target, replacement)
        }
        return this
    }
    FileMan replaceProperties(String target, String replacement){
        String targetPattern = target.replace('.','[.]').replace('$','\\$')
        String patternToGetProperty = "^\\s*" + targetPattern + "\\s*=.*\$"
        Matcher matchedList = Pattern.compile(patternToGetProperty, Pattern.MULTILINE).matcher(content)
        replacement = getRightReplacement("${target}=${replacement}")
        //LOG
        if (matchedList.size()){
            matchedList.eachWithIndex{ String from, int i ->
                logger.debug "from) ${from}"
                logger.debug "  to) ${replacement}"
            }
        }
        //REPLACE
        content = matchedList.replaceAll(replacement)
        return this
    }

    /*************************
     * replace YAML property
     *************************/
    FileMan replaceYaml(Map replacePropertyMap){
        if (replacePropertyMap)
            logger.debug "Yaml Property Replacement: ${replacePropertyMap.toMapString()}"
        replacePropertyMap.each{ String target, String replacement ->
            replaceYaml(target, replacement)
        }
        return this
    }
    FileMan replaceYaml(String targetPropertyName, String replacement){
        List propertyByDepthList = targetPropertyName.split('[.]').toList()
        int depthIndex = propertyByDepthList.size()

        List nowIndentByDepthList = []
        List nowPropertyByDepthList = []
        List<String> newLineList = []
        List<String> lineList = []
        String nowPrefix
        String nowSurfix
        int beforeIndent = 0

        content.eachLine{ lineList << it }
        for (int i=0; i<lineList.size(); i++){
            String thisLine = lineList[i]
            String newLine = thisLine
            String leftTrimLine = thisLine.replaceAll("^\\s+", "")
            String nowPropertyName
            String nowValue
            int nowIndent = thisLine.length() - leftTrimLine.length()

            //Analysis PropertyName
            int seperatorColonIndex = leftTrimLine.indexOf(':')
            if (!leftTrimLine.startsWith('#') && seperatorColonIndex != -1) {
                String propertyItem = leftTrimLine.substring(0, seperatorColonIndex)
                nowValue = leftTrimLine.substring(seperatorColonIndex+1)
                int checkExistIndentIndex = nowIndentByDepthList ? nowIndentByDepthList.indexOf(nowIndent) : -1

                //First
                if (nowIndent == 0) {
                    nowPropertyByDepthList = [propertyItem]
                    nowIndentByDepthList = [nowIndent]

                //sameDepth
                }else if (beforeIndent == nowIndent) {
                    nowPropertyByDepthList[(nowPropertyByDepthList.size() - 1)] = propertyItem
                    nowIndentByDepthList[(nowPropertyByDepthList.size() - 1)] = nowIndent

                //plusDepth
                }else if (beforeIndent < nowIndent) {
                    nowPropertyByDepthList << propertyItem
                    nowIndentByDepthList << nowIndent

                //minusDepth
                }else if (checkExistIndentIndex != -1){
                    nowPropertyByDepthList = nowPropertyByDepthList[0..checkExistIndentIndex]
                    nowPropertyByDepthList[(nowPropertyByDepthList.size()-1)] = propertyItem
                    nowIndentByDepthList = nowIndentByDepthList[0..checkExistIndentIndex]
                    nowIndentByDepthList[(nowIndentByDepthList.size()-1)] = nowIndent
                }

                //Make New Line
                nowPropertyName = nowPropertyByDepthList.join('.')
                if (nowPropertyName == targetPropertyName){
                    String newIndent = (nowIndent == 0) ? "" : (0..(nowIndent-1)).collect{' '}.join('')
                    newLine = "${newIndent}${propertyItem}: ${replacement}"
                    //Log
                    logger.debug "from) ${thisLine}"
                    logger.debug "  to) ${newLine}"
                }

                //Save Before
                beforeIndent = nowIndent
            }

            //Collect New Lines
            newLineList << newLine
        }

        content = newLineList.join(System.getProperty("line.separator"))
        return this
    }


    static String getRightReplacement(String replacement){
        // This Condition's Logic prevent disapearance \
        if (replacement.indexOf('\\') != -1)
            replacement = replacement.replaceAll('\\\\','\\\\\\\\')
        // This Condition's Logic prevent Error - java.lang.IllegalArgumentException: named capturing group has 0 length name
        if (replacement.indexOf('$') != -1)
            replacement = replacement.replaceAll('\\$','\\\\\\$')
        return replacement
    }



    /*************************
     * getFullPath
     *************************/
    static String getFullPath(String path){
        return getFullPath(nowPath, path)
    }

    static String getFullPath(File file){
        return getFullPath(nowPath, file?.path)
    }

    static String getFullPath(String nowPath_, String relativePath){
        nowPath_ = nowPath_ ?: FileMan.nowPath
        if (!relativePath)
            return null
        relativePath = toSlash(relativePath)
        if (startsWithRootPath(relativePath))
            return relativePath
        if (!startsWithRootPath(nowPath_))
            return (nowPath_ + relativePath)

        relativePath.split(/[\/\\]+/).each{ String next ->
            if (next.equals('..')){
                if (!isRootPath(nowPath_))
                    nowPath_ = (new File(nowPath_).isDirectory()) ? new File(nowPath_).getParent() : new File(nowPath_).getParentFile().getParent()
            }else if (next.equals('.')){
                if (!isRootPath(nowPath_))
                    nowPath_ = isFile(nowPath_) ? new File(nowPath_).getParent() : nowPath_
            }else if (next.equals('~')){
                nowPath_ = System.getProperty("user.home")
            }else{
                nowPath_ = "${nowPath_}/${next}"
            }
        }
        return toSlash(new File(nowPath_).path)
    }


    File getThisAppFile(){
        File thisAppFile
        CodeSource src = this.getClass().getProtectionDomain().getCodeSource()
        if (src)
            thisAppFile = new File( src.getLocation().toURI().getPath() )
        return thisAppFile
    }

    FileSetup getMergedOption(FileSetup opt){
        return globalOption.clone().merge(opt)
    }

    /*****
     * Get File's Extension
     *****/
    static String getExtension(File file){
        if (file)
            return getExtension(file.name)
        return null
    }

    static String getExtension(String filePath){
        String fileNameExtension
        if (filePath){
            int fileNameLastDotIndex = filePath.lastIndexOf('.')
            fileNameExtension = (fileNameLastDotIndex != -1) ? filePath.substring(fileNameLastDotIndex +1)?.toLowerCase() : ''
        }
        return fileNameExtension
    }




    /*****
     * Check Exclude File
     *****/
    static boolean isExcludeFile(File targetFile, List<String> excludePathList){
        List excludeCheckList = excludePathList.findAll{ String excludeItem ->
            File excludeFile = new File(excludeItem)
            return targetFile.path.equals(excludeFile.path)
        }
        return excludeCheckList
    }

    static boolean startsWithRootPath(String filePath){
        filePath = replaceWeirdString(filePath)

        //Simplely Check
        if (!filePath)
            return false

        if (filePath.startsWith('/') || filePath.startsWith('\\'))
            return true

        if (filePath.length() < 2)
            return false

        //Analisys Path
        String rootName
        List<String> fromOriginDepthList = filePath.split(/[\/\\]+/) - [""]
        rootName = fromOriginDepthList[0].toUpperCase()

        //Maybe Windows Driver Check
        if (rootName){
            if (filePath.startsWith('/') || filePath.startsWith('\\') || rootName.indexOf(':') != -1) {
                List<File> rootList = new File('.').listRoots()
                if (rootList.findAll { (it as String).startsWith(rootName) })
                    return true
                else
                    return false
            }
        }
        return false
    }

    static boolean isDifferentRootDir(String from, String to){
        boolean result = false
        if (startsWithRootPath(from) && startsWithRootPath(to)){
            List<String> fromDepthList = getLastDirectoryPath(from).split(/[\/\\]/) - [""]
            List<String> toDepthList = getLastDirectoryPath(to).split(/[\/\\]/) - [""]
            if (fromDepthList[0] != toDepthList[0])
                result = true
        }
        return result
    }

    static String replaceWeirdString(String string){
        byte[] bytes = string.getBytes()
        int length = bytes.length
        if (length > 3 && bytes[0] == -30 && bytes[1] == -128 && bytes[2] == -86)
            string = new String(bytes, 3, length -3)
        return string
    }


    /**
     * Seperator with comma or space
     */
    static List<String> toList(String itemsSeperatedWithCommaOrSpace){
        List<String> itemList = (itemsSeperatedWithCommaOrSpace) ? itemsSeperatedWithCommaOrSpace.replaceAll('[,]', ' ').split(/\s{1,}/) : []
        return itemList
    }

    /**
     * 특정 필드로 LIST 조각내서 Map에 담기
     * List -> Map<String, List> by Key
     */
    static Map toMap(List dataList, String keyName){
        Map<String, List> fileMapForSeperator = [:]
        // 4-1. fileMapForSeperator에 특정 필드로 분류해서 List로서 저장
        dataList.each{
            String seperator = it[keyName]
            if (!fileMapForSeperator[seperator])
                fileMapForSeperator[seperator] = []
            fileMapForSeperator[seperator] << it
        }
        return fileMapForSeperator
    }

    /**
     * 특정 필드로 LIST 조각내서 Map에 담기
     * List -> Map<String, List> by Key
     */
    static Map toMap(List dataList, String keyName, List<String> validKeyList){
        Map<String, List> fileMapForSeperator = [:]
        // 4-1. fileMapForSeperator에 특정 필드로 분류해서 List로서 저장
        validKeyList.each{ String validKey ->
            fileMapForSeperator[validKey] = dataList.findAll{ it[keyName] == validKey }
        }
        return fileMapForSeperator
    }

    static toSlash(String path){
        return path?.replaceAll(/[\/\\]+/, '/')
    }

    static toBlackslash(String path){
        return path?.replaceAll(/[\/\\]+/, '\\\\')
    }

    static toBlackslashTwice(String path){
        return path?.replaceAll(/[\/\\]+/, '\\\\\\\\')
    }

}
