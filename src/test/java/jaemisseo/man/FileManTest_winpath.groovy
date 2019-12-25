package jaemisseo.man

import org.junit.Test

class FileManTest_winpath {


    @Test
    void simple(){
        String path = './test/path/just/temp'

        // \\는 (실제로) \
        assert FileMan.toBlackslash(path) == '.\\test\\path\\just\\temp'

        // \\\\는 (실제로) \\
        assert FileMan.toBlackslashTwice(path) == '.\\\\test\\\\path\\\\just\\\\temp'
    }

}
