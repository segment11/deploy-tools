#!/bin/sh
#
#################################################################
#
# Start script for MemDB
#
#################################################################

if [ "X$SERVER_HOME" = "X" ]
then
    # shellcheck disable=SC2046
    SERVER_HOME=$(dirname $(readlink -f $0))
    if [ ! -f "${SERVER_HOME}/etc/cfg.xml" ]
    then
         SERVER_HOME=$(dirname $SERVER_HOME)
    fi
fi

if [ "X$JAVA_HOME" = "X" ]
then
    JAVA_HOME=/usr/;export JAVA_HOME
fi


#################################################################

SERVER_NAME="MemoryDB"

#################################################################

wait4pid() {
  pid=$1
  time=$2

  if [ "X$pid" = "X" ]
  then
    echo "null pid"
    return
  fi

  i="0"
  while [ "$i" -le "$time" ]
  do
    i=$((i+1))
    sleep 1
    echo waited $i seconds
    newpid=`ps -ef | awk '{print $2}' | grep $pid`
    if [ "$pid" != "$newpid" ]
    then
      echo "process $pid stoped"
      exit 0
    fi
  done
}

#################################################################


# shellcheck disable=SC2006
# shellcheck disable=SC2045
for file in `ls "${SERVER_HOME}"/lib`
do
    if echo "$file" | grep -q -E '\.jar$'
    then
        if [ -f "${SERVER_HOME}/lib/${file}" ]
        then
            CLASS_PATH=${SERVER_HOME}/lib/${file}:${CLASS_PATH}
        fi
    fi
done

if [ "$1" != "stop" ]
then
    if [ -f ${SERVER_HOME}/bin/external.vmoptions ]
    then
        SERVEROPT=`cat ${SERVER_HOME}/bin/external.vmoptions | grep -v '^[	 ]*#' | xargs`
    fi

    SERVEROPT=" -Dserver=${SERVER_NAME} -Dserver.home=${SERVER_HOME} -Djdk.tls.rejectClientInitiatedRenegotiation=true ${SERVEROPT} "

else
    echo "stop server: home=${SERVER_HOME}"

    pidcmd="ps -ef | grep -v grep | grep 'server=${SERVER_NAME} ' | grep 'server.home=${SERVER_HOME} '"

    pid=`eval $pidcmd | awk '{print $2}'`

    if [ "$pid" != "" ]
    then
        echo "The PID of the stopped process is $pid"
        kill $pid

        wait4pid "$pid" 1800

        echo "stop the process force"
        kill -9 $pid
    fi
    echo "Server stoped."
    exit 1
fi

JAVA=`which java`

if [ "X${JAVA}" = "X" ]
then
    JAVA=${JAVA_HOME}/bin/java
fi

if [ ! -f "${JAVA}" ] ; then
    echo ERROR: JAVA was not found
    exit 1
fi

# shellcheck disable=SC2006
JAVA_VERSION=`"${JAVA}" -version 2>&1 | awk -F '"' '/version/ {print $2}'`
# shellcheck disable=SC2006
MAJOR_VERSION=`echo "$JAVA_VERSION" | cut -d '.' -f 1`
# MINOR_VERSION=$(echo "$JAVA_VERSION" | cut -d '.' -f 2)
if [ "$MAJOR_VERSION" -ge 11 ]; then
    ADD_OPTIONS=" --add-opens=java.base/sun.security.util=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/sun.security.x509=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/sun.security.pkcs12=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/sun.security.pkcs=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/sun.security.action=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/sun.security.provider=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/sun.security.provider.certpath=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/jdk.internal.access=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} --add-opens=java.base/java.nio=ALL-UNNAMED "
    ADD_OPTIONS="${ADD_OPTIONS} -Dio.netty.tryReflectionSetAccessible=true "
    echo "Using Java version: ${MAJOR_VERSION}"
    #echo "Add parameters: $ADD_OPTIONS"
else
    #echo "Java version: ${MAJOR_VERSION}, does not require additional option"
    ADD_OPTIONS=""
fi

cd ${SERVER_HOME}

if [ "$1" != "daemon" ]
then
  XXXX $JAVA ${SERVEROPT} ${ADD_OPTIONS} -classpath "$CLASS_PATH" com.server.Server $1
else
 nohup $JAVA ${SERVEROPT} ${ADD_OPTIONS} -classpath "$CLASS_PATH" com.server.Server > /dev/null 2>&1 & > /dev/null
fi