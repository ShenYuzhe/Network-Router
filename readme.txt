Network Router Simulator
1. installation
in router/ directory
1) “make clean” in “ys2821_java/router” folder to clean all the compiled files
2) “make all” in “ys2821_java/router” folder to compile all the code
3) “make run <config_x.txt>” in “ys2821_java/router” folder to start each “host”,
**the parameter after "make run" should be the configfile for this client,
**every config file should be put in "/ys2821_java/router/"
**FIlE to be transfered should be put in the "/ys2821_java/router/"
**FILE received will be saved in "/ys2821_java/router/output/",
	if the "output" does not exist in the "/ys2821_java/router/", you should create one.
**The file received will be of the same name as the file sent.


2. OPERATIONs
SETTHRESHOLD <Threshold (this should be an integer)>
			// **In most situations My Distance Vector converges natually, this Threshold is only used for some very special cases.
			// This is the extra command I implemented (not for bonus).
			// To help converge quicker, I implemented an upperbound. 
			// If distance from a specific host to another exceeds this Threshold, the distance will be discarded.
			// The default value is 200.
			// To set it manually I also implemented this extra command.
			// You can set a larger Threshold if you want to implement a topology with larger distances.
			// Or you can set a smaller Threshold if you want to implement a topology with smaller distances.
			// This extra command I implement to set the upperbound distance for a specific host.
			// e.g. if you type SETTHRESHOLD 50 in router_0 then any distance over 50 will be discarded.
			// With this command you can set a proper upperbound depending on the topology to discard "too large" distances.
LINKDOWN <ip_address> <port>
LINKUP <ip_address> <port>
CHANGECOST <ip_address> <port> <cost>
SHOWRT
CLOSE
TRANSFER <file_name> <destination_ip> <port>
ADDPROXY <proxy_ip> <proxy_port> <neighbor_ip> <neighbor_port>
REMOVEPROXY <neighbor_ip> <neighbor_port>

3. Implementation
// Note it is implemented with 2 reliable file transfer bonus feature
// including “ACK” and “checksum”
There are totally 5 packets contained in this project:
1) hostManager:
	MainHost.java // main entrance of the project
2) applicationLayer:
	AppLayer.java // main class for Application Layer
		      // calling APIs of Transport Layer and Network Layer
	
	CommandReader.java 	// reading and analyze all the commands entered.
	ApplicationAction.java 	// internal class used to describe a command
	ApplicationCallback.java // called by transport Layer or CommandReader.
3) transportLayer:
	TransLayer.java		// main class for transport layer
	TimoutChecker.java	// check whether there is timeout Packet.
	TransportHeader.java 	// describes a Transport Layer header.
				// stores the header information for file sender.
				// stores the file sending status
				// stores saved “bytes copy” for previous packets for resending.
				// this class is scanned by TimeoutChecker to watch the 
				// file transferring status for each transfer of file
	CacheBlock		// stores the file status for file receiver side.
	TransportCallback // callback class called by TimeoutChecker and Network layer.
4) networkLayer:
	NetLayer.java	// main class for Network layer.
	DistanceManager	// implementation of Distance Vector algorithm
			// offers APIs to be called by NetLayer to modify network topology
	PacketReceiver.java // manages all the communication functions including receiving and sending packets.
	TimeManager.java// scans all the Network events, if there is one times out, call the Network callback to tell NetLayer.java
	NetworkEvent	// each instance of NetworkEvent represents a timeout event either for “route_update” or “neighbor_timout”
			// it is scanned by TimeManager
	NetworkCallback	// callback class called by TimeManager.
	node 		// represents a neighbor or a remote node.
	NetworkHeader	// describes a Network Layer header.
5) byteUtilLayer:
	ByteHandler	// calculate checksum and checks whether the sequence of bytes is corrupted.

4. packets format
	<2 byte network_header checksum> <128 byte network_header bytes> 
	<2 byte transport_header checksum> <128 byte transport_header bytes> 
	<2 byte transport_data checksum> <1024 byte transport_data bytes>

5. header format
Network Layer header:	<type “route_update/user_data”>;<source address>;<destination address>#<data_field>
//Note: the information before “#” is the actual header.
	“route_update” route_update;<source address>;<destination address>#<weight>;<address_1>&distance_1,<address_2>&distance_2
	“cost_change” cost_change;<source address>;<destination address>#<cost>
	“link_down” link_down;<source address>;<destination address>#null
	“link_up” link_up;<source address>;<destination address>#null
	“dead_node” dead_node;<source address>;<destination address>#<dead_node_address> // this is the extra message to propagate a dead node
										       // to far distance;
	“user_data” user_data;<source address>;<destination address>#<transport data>							
	“user_ack” user_data;<source address>;<destination address>#<transport data>  // for both “ack” and “nak”
Transport Layer header: <type “data/ack/nak”>;<sequence number>;<is End>;<data segment length>;<file name>#data field
//Note: the information before “#” is the actual header.
	“ack” ack;<sequence number>;<is End>;<data segment length>;<file name>#null
	“nak” nak;<sequence number>;<is End>;<data segment length>;<file name>#null
	“data” data;<sequence number>;<is End>;<data segment length>;<file name>#data segment

6. Protocal
	Network Layer: its Vector Distance algorithm implemented with poisoned reverse.
	TransportLayer: (window size is 1)
		Sender:
			if (isCorrupt(NetworkHeader) || isCorrupt(TransportHeader))
				drop
			else if (ACK.seq == current.seq)
				send next;
			else if (ACK.seq != current.seq)
				ignore
			else if (timeout || NAK)
				resend
		Receiver:
			if (isCorrupt(NetworkHeader) || isCorrupt(TransportHeader))
				drop
			else if (Packet.seq != expect_eq)
				send(“ACK”)
			else if (isCorrupt(data segment))
				send(“NAK”)
			else
				send(“ACK”)
6 test cases:
timeout values:
	“route_update” 10s interval
	“neighbor timeout” 30s interval
	“packet timeout” 3s interval

File to be transferred
	“cat.jpg”	file size: 1289142 bytes;
(Since the timeout value for retransmission is set to 3 seconds, if proxy is added, it takes quite a long time (about 5~7 minutes) to transmit)
(if proxy is not added, it will take about 20 ~ 30 seconds to finish transmit)

		
6 “config.txt” are as following:
	node_1:
	7000 10
	<IP>:2000 2.0
	<IP>:3000 1.1

	node_2:
	2000 10
	<IP>:7000 2
	<IP>:3000 2
	<IP>:5000 3
	
	node_3:
	3000 10
	<IP>:7000 1
	<IP>:2000 2
	<IP>:4000 1
	<IP>:5000 3
	
	node_4:
	4000 10
	<IP>:3000 1
	<IP>:5000 1
	<IP>:6000 2	

	node_5:
	5000 10
	<IP>:2000 3
	<IP>:3000 3
	<IP>:4000 1
	<IP>:6000 5
	
	node_6:
	8000 10
	<IP>:6000 2
	<IP>:4000 3		
	
	

