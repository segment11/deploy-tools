package samples

class TongRdsStarterContent {
    static String starterSh(int ioThreads) {
        def tasksetPrefix = 'taskset -c 0 '
        if (ioThreads == 2) {
            tasksetPrefix = 'taskset -c 0,1 '
        } else if (ioThreads == 4) {
            tasksetPrefix = 'taskset -c 0,1,2,3 '
        }

        def starterFile = new File('starter.sh')
        def text = starterFile.text
        text.replace('XXXX', tasksetPrefix)
    }
}
