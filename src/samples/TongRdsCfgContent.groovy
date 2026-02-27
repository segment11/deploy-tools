package samples

class TongRdsCfgContent {
    static String cfgContent(int ioThreads){
        """
<?xml version="1.0" encoding="UTF-8"?>
<Server>
    <Common>
        <RuntimeModel>debug</RuntimeModel>
        <DataDump>\${Server.Common.DataDump:10}</DataDump>
        <DataDumpAppending>\${Server.Common.DataDumpAppending:false}</DataDumpAppending>
        <StartWaiting>\${Server.Common.StartWaiting:5}</StartWaiting>
        <Service>\${Server.Common.Service:WebSession}</Service>
        <MaxKeyLength>\${Server.Common.MaxKeyLength:1m}</MaxKeyLength>
        <MaxValueLength>\${Server.Common.MaxValueLength:10m}</MaxValueLength>
        <DataDumpProcessors>\${Server.Common.DataDumpProcessors:1}</DataDumpProcessors>
    </Common>
    <Log>
        <!-- nothing, error, warn, info, debug, dump. >
        <    error is the default                      -->
        <Level>\${Server.Log.Level:error}</Level>
        <!-- Log retention days -->
        <BackDates>\${Server.Log.BackDates:30}</BackDates>
    </Log>
    <Listen>
        <Port>6200</Port>
        <Threads>${ioThreads}</Threads>
        <MaxConnections>\${Server.Listen.MaxConnections:1000}</MaxConnections>
        <Backlog>1024</Backlog>
        <!-- 0: telnet; 1: SSL; 2: password; 3: SSL + password. default is 1 -->
        <Secure>\${Server.Listen.Secure:0}</Secure>
        <Password>\${Server.Listen.Password:537cb0e6b7fbad3b75f2245e61b4d2e4}</Password>
        <RedisPort>6379</RedisPort>
        <RedisPlainPassword>\${Server.Listen.RedisPlainPassword:true}</RedisPlainPassword>
        <RedisPassword>\${Server.Listen.RedisPassword:123456}</RedisPassword>
    </Listen>
    <Tables>\${Server.Tables:1}</Tables>
    <TableTemplate>
        <Blocks>\${Server.TableTemplate.Blocks:4}</Blocks>
        <Rows>\${Server.TableTemplate.Rows:0}</Rows>
        <Key>bytes2, 100</Key>
        <Value>variable, 0</Value>
        <Indexes>0</Indexes>
        <Sync>
            <ListNumbers>\${Server.TableTemplate.Sync.ListNumbers:1}</ListNumbers>
            <ListLength>\${Server.TableTemplate.Sync.ListLength:30000}</ListLength>
        </Sync>
    </TableTemplate>
</Server>
""".trim()
    }
}
