package samples

import deploy.DeploySupport
import deploy.OneCmd
import deploy.RemoteInfo
import groovyjarjarpicocli.CommandLine
import redis.clients.jedis.Jedis

import java.util.concurrent.Callable

@CommandLine.Command(name = "java -jar deploy-tools-1.0.0.jar", version = "1.0.0",
        description = "Do perf benchmark use memtier_benchmark.")
class DoPerfBenchmark implements Callable<Integer> {
    static List<String> generateCmdWithArgs(int seconds, int range, int clients, int threads, String host, int port, int valueLength, String ratio = '5:5') {
        """
memtier_benchmark
-h
${host}
-p
${port}
-t
${threads}
-c
${clients}
--test-time
${seconds}
--hide-histogram
--key-prefix="key:"
--key-minimum=1
--key-maximum=${range}
--ratio=${ratio}
--data-size=${valueLength}
--distinct-client-seed
""".toString().trim().split('\n').toList()
    }

    static String startRedisServerCmdLine(String binDir, String serverCmd = 'redis-server', int ioThreads = 1) {
        "${binDir}/${serverCmd} --save \"\" --appendonly no --protected-mode no --io-threads ${ioThreads}".toString()
    }

    static String startTongRdsServerCmdLine(String binDir, String serverCmd = 'StartServer.sh') {
        "${binDir}/${serverCmd}".toString()
    }

    @CommandLine.Option(names = ['-h', '--host'], description = 'host, eg: localhost, default: localhost')
    String host = 'localhost'

    @CommandLine.Option(names = ['-p', '--port'], description = 'port, eg: 6379, default: 6379')
    int port = 6379

    @CommandLine.Option(names = ['-s', '--seconds'], description = 'seconds, eg: 30, default: 30')
    int seconds = 30

    @CommandLine.Option(names = ['-r', '--range'], description = 'range, eg: 1000000, default: 1000000')
    int range = 1000000

    @CommandLine.Option(names = ['-c', '--clients'], description = 'clients, eg: 1, default: 50')
    int clients = 50

    @CommandLine.Option(names = ['-t', '--threads'], description = 'threads, eg: 1, default: 4')
    int threads = 4

    @CommandLine.Option(names = ['-v', '--values-lengths'], description = 'values-lengths, eg: 20,50,100, default: 100')
    String valuesLengths = '100'

    @CommandLine.Option(names = ['-R', '--ratios'], description = 'ratios, eg: 5:5,2:8, default: 2:8')
    String ratios = '2:8'

    // server options
    @CommandLine.Option(names = ['-T', '--ioThreads'], description = 'ioThreads, eg: 1,2,4, default: 1')
    String ioThreadsStr = '1'

    // server ssh options
    @CommandLine.Option(names = ['-u', '--user'], description = 'user, eg: ec2-user, default: ec2-user')
    String user = 'ec2-user'

    @CommandLine.Option(names = ['-pw', '--password'], description = 'password, eg: <PASSWORD>, default: <PASSWORD>')
    String password

    @CommandLine.Option(names = ['-o', '--output-file-path'], description = 'output-file-path, eg: output.txt, default: output.txt')
    String outputFilePath = 'output.txt'

    static void main(String[] args) {
        def exitCode = new CommandLine(new DoPerfBenchmark()).execute(args)
        System.exit(exitCode)
    }

    static String getFromProperty(String key, String defaultValue) {
        def value = System.getProperty(key)
        value ?: defaultValue
    }

    File outputFile

    @Override
    Integer call() throws Exception {
        def userHome = System.getProperty('user.home')
        def redisServerBinDir = getFromProperty('redis.server.bin.dir', userHome + '/bin')
        def engulaServerBinDir = getFromProperty('engula.server.bin.dir', userHome + '/bin')
        def tongRdsServerBinDir = getFromProperty('tong.server.bin.dir', userHome + '/pmemdb/bin')

        outputFile = new File(outputFilePath)
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }

        for (ratio in ratios.split(',')) {
            for (valueLength in valuesLengths.split(',')) {
                for (ioThreads in ioThreadsStr.split(',')) {
//                    testOneServer(ratio, valueLength as int, ioThreads as int, 'redis-server', redisServerBinDir)
//                    testOneServer(ratio, valueLength as int, ioThreads as int, 'engula-server', engulaServerBinDir)
                    testOneServer(ratio, valueLength as int, ioThreads as int, 'StartServer.sh', tongRdsServerBinDir)
                }
            }
        }
        0
    }

    boolean testOneServer(String ratio, int valueLength, int ioThreads, String serverCmd, String binDir) {
        isServerLocal = host == 'localhost'
        def isTongRds = 'StartServer.sh' == serverCmd

        def line = "Testing: ${isTongRds ? 'TongRds' : serverCmd}, ratio: ${ratio}, valueLength: ${valueLength}, ioThreads: ${ioThreads}"
        println line
        outputFile.append(line + '\n')

        if (isTongRds) {
            def serverCmdLine = startTongRdsServerCmdLine(binDir, serverCmd)
            // change io threads in config file, todo
            if (isServerLocal) {
                // change cfg.xml local
                def cfgContent = TongRdsCfgContent.cfgContent(ioThreads)
                def binFile = new File(binDir, serverCmd)
                def cfgDir = new File(binFile.parentFile.parentFile, 'etc')
                def cfgFile = new File(cfgDir, 'cfg.xml')
                cfgFile.text = cfgContent
                println 'update cfg.xml, set io threads to ' + ioThreads + ', file path: ' + cfgFile.absolutePath
            } else {
                // change cfg.xml remote
                def tmpFile = new File('/tmp/cfg.xml')
                def cfgContent = TongRdsCfgContent.cfgContent(ioThreads)
                tmpFile.text = cfgContent

                def info = remoteInfo()
                def deploy = DeploySupport.instance

                def remotePath = binDir.replace('bin', 'etc') + '/cfg.xml'
                deploy.send(info, tmpFile.absolutePath, remotePath)
                println 'done send cfg.xml to remote, set io threads to ' + ioThreads + ', remote path: ' + remotePath
            }
            startServer(serverCmdLine)
        } else {
            def serverCmdLine = startRedisServerCmdLine(binDir, serverCmd, ioThreads)
            startServer(serverCmdLine)
        }

        def isListening = waitServerListening()
        if (!isListening) {
            println 'server not listening, please check'
            return false
        }

        def clientCmdWithArgs = generateCmdWithArgs(seconds, range, clients, threads, host, port, valueLength, ratio)
        startClient(clientCmdWithArgs)

        clearStartedServer(generateKillServerCmd(serverCmd))
        true
    }

    Process serverProcessLocal

    boolean isServerLocal = true

    RemoteInfo remoteInfo() {
        def info = new RemoteInfo()
        info.host = host
        info.user = user
        info.password = password
        info.isUsePass = true
        info.port = 22
        info
    }

    boolean startServer(String serverCmdLine) {
        println serverCmdLine
        if (isServerLocal) {
            println 'start server on localhost, need not ssh'
            serverProcessLocal = ['bash', '-c', serverCmdLine].execute()
            serverProcessLocal.consumeProcessOutput(System.out, System.err)
            Thread.start {
                serverProcessLocal.waitFor()
            }
            Thread.sleep(1000 * 5)
            return true
        }

        def info = remoteInfo()
        def deploy = DeploySupport.instance

        def cmd = OneCmd.simple(serverCmdLine)
        deploy.exec(info, cmd)
    }

    static String generateKillServerCmd(String serverCmd) {
        if ('redis-server' in serverCmd) {
            return "killall redis-server"
        } else if ('engula-server' in serverCmd) {
            return "killall engula-server"
        } else {
            // tong rds, kill java process
            return "killall -9 java"
        }
    }

    void clearStartedServer(String killServerCmdWhenRemote) {
        if (serverProcessLocal) {
            def isTongRds = 'java' in killServerCmdWhenRemote
            if (isTongRds) {
                // kill process force, wait too much time '-Dserver=MemoryDB'
                def killCmdLine = 'kill -9 $(ps -ef | grep "java.*-Dserver=MemoryDB" | grep -v grep | awk \'{print $2}\')'
                println killCmdLine
                def pp = ['bash', '-c', killCmdLine].execute()
                pp.consumeProcessOutput(System.out, System.err)
                pp.waitFor()
                println 'server process local killed'
            } else {
                serverProcessLocal.destroy()
                println 'server process local destroyed'
                serverProcessLocal = null
            }

            Thread.sleep(1000 * 5)
            return
        }

        def info = remoteInfo()
        def deploy = DeploySupport.instance

        def cmd = OneCmd.simple(killServerCmdWhenRemote)
        deploy.exec(info, cmd)
        println 'server process remote killed'
        Thread.sleep(1000 * 5)
    }

    boolean startClient(List<String> clientCmdWithArgs) {
        def clientCmdLine = clientCmdWithArgs.join(' ')
        println clientCmdLine
        def process = ['bash', '-c', clientCmdLine].execute()
        def exitCode = process.waitFor()
        if (exitCode != 0) {
            println 'start client failed, exit code: ' + exitCode + ', err msg: ' + process.err.text
            return false
        }

        def text = process.text
        outputFile.append(text)
        true
    }

    boolean waitServerListening() {
        for (int i = 0; i < 10; i++) {
            try {
                def jedis = new Jedis(host, port)
                jedis.ping()
                jedis.close()
                println 'server ready'
                return true
            } catch (Exception e) {
                println 'waiting for server ready, retry..., ' + e.message
                Thread.sleep(1000 * 5)
            }
        }
        false
    }
}
