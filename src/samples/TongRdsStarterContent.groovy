package samples

class TongRdsStarterContent {
    static String starterSh(int ioThreads) {
        def starterFile = new File('starter.sh')
        def text = starterFile.text
        text.replace('XXXX', ioThreads + '')
    }
}
