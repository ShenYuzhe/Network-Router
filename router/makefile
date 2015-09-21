JAVAC = javac
JAVA = java
PACKAGEHOST = hostManager/
PACKAGEAPP = applicationLayer/
PACKAGETRANS = transportLayer/
PACKAGENET = networkLayer/
PACKAGEBYTE = byteUtil/

sources = ${PACKAGEHOST}MainHost.java \
	${PACKAGEAPP}AppLayer.java ${PACKAGEAPP}ApplicationCallback.java ${PACKAGEAPP}CommandReader.java \
	${PACKAGEAPP}ApplicationAction.java \
	${PACKAGETRANS}TransLayer.java ${PACKAGETRANS}TransportCallback.java \
	${PACKAGETRANS}CacheBlock.java ${PACKAGETRANS}TimeoutChecker.java \
	${PACKAGETRANS}TransportHeader.java \
	${PACKAGENET}DistanceManager.java ${PACKAGENET}NetLayer.java \
	${PACKAGENET}NetworkCallback.java ${PACKAGENET}NetworkEvent.java \
	${PACKAGENET}NetworkHeader.java ${PACKAGENET}node.java \
	${PACKAGENET}PacketReceiver.java ${PACKAGENET}TimeManager.java \
	${PACKAGEBYTE}ByteHandler.java

CLASSES = $(sources:.java=.class)

all: $(CLASSES)

clean :
	rm -f ${PACKAGEHOST}*.class ${PACKAGEAPP}*.class \
		${PACKAGETRANS}*.class ${PACKAGENET}*.class \
		${PACKAGEBYTE}*.class

ifeq (run,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(ARGS):;@:)
endif
run: 
	$(JAVA) ${PACKAGEHOST}MainHost ${ARGS}

 %.class : %.java
	$(JAVAC) $<
