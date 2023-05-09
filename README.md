# deploy-tools
A java ssh tool that can execute command and shell using com.jcraft.jsch.

## Sample
```groovy
package deploy

def deploy = DeploySupport.instance

def info = new RemoteInfo()
info.host = '127.0.0.1'
info.user = 'kerry'
info.password = '****'
info.isUsePass = true
info.rootPass = '****'
info.port = 22

def userHomeDir = '/home/' + info.user

def localFilePath = userHomeDir + '/test.txt'
def remoteFilePath = userHomeDir + '/test_dest.txt'
deploy.send(info, localFilePath, remoteFilePath)

def cmd = OneCmd.simple('pwd')
println deploy.exec(info, cmd)
println cmd.result
println 'execute command result: ' + cmd.ok()

def list = [new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(info.user + '@'))]
if ('root' != info.user) {
    list << new OneCmd(cmd: 'su', maxWaitTimes: 10, checker: OneCmd.keyword('Password', '密码'))
    list << new OneCmd(cmd: info.rootPass, maxWaitTimes: 10, showCmdLog: false,
            checker: OneCmd.keyword('root@').failKeyword('failure'))
}
list << new OneCmd(cmd: 'mkdir -p /data/tmp', maxWaitTimes: 10, checker: OneCmd.any())
list << new OneCmd(cmd: 'apt install -y docker.io', maxWaitTimes: 300,
        checker: OneCmd.keyword(' 0 newly installed', 'Processing triggers', ':' + userHomeDir)
                .failKeyword('Permission', 'broken packages'))
println 'execute shell result: ' + deploy.exec(info, list, 60, true)
```

## Output
```text
11:56:41.512 INFO  - jsch session connected 127.0.0.1
11:56:41.590 INFO  - sftp channel connected 127.0.0.1
11:56:41.593 INFO  - Try to start progress monitor.
11:56:41.594 INFO  - Progress monitor started.
11:56:41.594 INFO  - Transfer end.
11:56:41.597 INFO  - scp cost 3ms to /home/kerry/test_dest.txt
11:56:41.668 INFO  - jsch session connected 127.0.0.1
11:56:41.669 INFO  - <- pwd timeout: 10000ms
11:56:41.950 INFO  - remote exec 127.0.0.1 command pwd cost 279ms 
status 0 result /home/kerry

true
/home/kerry

execute command result: true
11:56:41.999 INFO  - jsch session connected 127.0.0.1
11:56:42.076 INFO  - <- pwd timeout: 12000ms
11:56:42.076 INFO  - wait a while
11:56:42.277 INFO  - -> Welcome to Kylin V10 SP1 (GNU/Linux 5.19.0-41-generic x86_64)

 * Documentation:  https://help.ubuntu.com
 * Management:     https://landscape.canonical.com
 * Support:        https://ubuntu.com/advantage

扩展安全维护（ESM）Applications 未启用。

6 更新可以立即应用。
要查看这些附加更新，请运行：apt list --upgradable

启用 ESM Apps 来获取未来的额外安全更新
See https://ubuntu.com/esm or run: sudo pro status

Last login: Tue May  9 11:55:24 2023 from 127.0.0.1
pwd
kerry@kerry-office:~$ pwd
/home/kerry
kerry@kerry-office:~$ 
11:56:42.277 INFO  - <- su timeout: 2000ms
11:56:42.277 INFO  - wait a while
11:56:42.478 INFO  - -> su
密码： 
11:56:42.478 INFO  - <- *** timeout: 2000ms
11:56:42.478 INFO  - wait a while
11:56:42.594 INFO  - Transfer done. Cancel timer.
11:56:42.594 INFO  - Try to stop progress monitor.
11:56:42.594 INFO  - Progress monitor stopped.
11:56:42.678 INFO  - -> 
root@kerry-office:/home/kerry# 
11:56:42.678 INFO  - <- mkdir -p /data/tmp timeout: 2000ms
11:56:42.678 INFO  - wait a while
11:56:42.878 INFO  - -> mkdir -p /data/tmp
root@kerry-office:/home/kerry# 
11:56:42.878 INFO  - <- apt install -y docker.io timeout: 60000ms
11:56:42.878 INFO  - wait a while
11:56:43.079 INFO  - -> apt install -y docker.io
正在读取软件包列表... 完成
正在分析软件包的依赖关系树... 完成
正在读取状态信息... 完成
docker.io 已经是最新版 (20.10.21-0ubuntu1~22.04.3)。

11:56:43.279 INFO  - -> 升级了 0 个软件包，新安装了 0 个软件包，要卸载 0 个软件包，有 6 个软件包未被升级。
root@kerry-office:/home/kerry# 
execute shell result: true

Process finished with exit code 0
```
