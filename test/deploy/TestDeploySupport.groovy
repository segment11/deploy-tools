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
