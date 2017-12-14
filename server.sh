#!/bin/sh
BASEDIR=`dirname $0`
exec java \
	-XstartOnFirstThread \
	-Xmx512M -XX:MaxPermSize=128M \
	-classpath $BASEDIR/swt/swt.jar:$BASEDIR \
	-Djava.library.path=$BASEDIR/swt \
	ostrowski.combat.server.CombatServer
