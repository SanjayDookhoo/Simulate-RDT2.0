import java.net.*;
import java.io.*;
import java.util.*;

class Rdt{

	public byte calcChecksum(byte[]  data){
		byte maxVal;
		byte sum=0;
		// foreach(byte b : data){
		// 	sum+=b;
		// }]

		for(int i=0;i<data.length;i++){
			sum+=data[i];
		}

		//set maxVal to be max of a byte
		//maxVal = maxVal | (1 << pos);

		//sum=maxVal - sum;
		return sum;
	}

	public byte[] concatByteArr(byte[] a, byte[] b){
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);

		return c;
	}
}

class Rdt_server extends Rdt{
	private byte sum;
	private byte checkSum;
	private int seqExpected;
	private int seqReceived;
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;
	private byte[] sendData = new byte[1024];
  	private byte[] receiveData = new byte[1024];

	//rdt_send and rdt_rcv of the server is masked under this function
	public void udp_send(ClientNode currentClient,byte[] data,DatagramSocket serverSocket,InetAddress IPAddress,int port){
		rdt_send(currentClient,data,serverSocket,IPAddress,port);
		while(!rdt_rcv(serverSocket)){
			rdt_send(currentClient,data,serverSocket,IPAddress,port); //packet is resent continuously until it sends properly
		}

		//since packet was successfully sent, change seq of currentClient
		if(currentClient.getSeq()==1) currentClient.setSeq(0);
		else currentClient.setSeq(1);
	}

	//sends data to client
	//most likely need to include lenght also
	public void rdt_send(ClientNode currentClient,byte[] data,DatagramSocket serverSocket,InetAddress IPAddress,int port){//format: checksum,seq,data          takes currentClient to push to its queue, send from the queue, and determine seq number at
		int seq = currentClient.getSeq();
		seqExpected=currentClient.getSeq();

		//create pkt using seq number of client
		byte [] sndpkt = make_pkt(seq,data);

		//sends packet using udp datagram
		sendPacket = new DatagramPacket(sndpkt, sndpkt.length, IPAddress, port);
		try{
			serverSocket.send(sendPacket);
		}catch(Exception e){System.out.println("Exception sending from server to client");}
	}

	//received acknowedgement from client, if true then data received correctly and in the right sequence, otherwise flase
	public boolean rdt_rcv(DatagramSocket serverSocket){//format: seq,checksum
		boolean isAck=true;
		boolean corrupt=false;
		byte expectedChecksum;
		byte receivedChecksum;
		byte checksum;
		byte [] rcvpkt;


		//receive packet using udp datagram
		receivePacket = new DatagramPacket(receiveData, receiveData.length);

		try{
			serverSocket.receive(receivePacket);
		}catch(Exception e){System.out.println("Exception receiving from client to server");}
		rcvpkt=receivePacket.getData();

		//if corrupted,checksum dont match
		expectedChecksum=rcvpkt[0];
		//removing checksum header
		rcvpkt=Arrays.copyOfRange(rcvpkt,1,rcvpkt.length); //rcvpkt.lenght -1 ?
		receivedChecksum=calcChecksum(rcvpkt);
		if(receivedChecksum!=expectedChecksum){
			corrupt=true;
		}

		//if isAck, seq received is what was sent
		seqReceived=rcvpkt[0];
		if(seqReceived!=seqExpected){
			isAck=false;
		}

		if(corrupt || !isAck) {
			if(corrupt){
				System.out.println("\nTRANSFER ERROR: Packet(ACK) sent from client was corrupted");
			}else if(!isAck){
				System.out.println("\nTRANSFER ERROR: Packet(ACk) sent from client was not of the right sequence expected");

				System.out.println(seqExpected);
				System.out.println(seqReceived);
			}

			return false;
		}

		System.out.println("ack received");
		return true;

	}

	public byte[] make_pkt(int seq,byte[] data){//format: checksum,seq,data
		byte[] b = new byte[1];
		byte[] checksum = new byte[1];
		b[0]=(byte)seq;

		//concat seq and data
		byte[] a=concatByteArr(b,data);

		//calc sum
		checksum[0]= calcChecksum(a);

		//concat check sum with data and header(seq)
		byte [] sndpkt = concatByteArr(checksum,a);

		RandomChooseToCorrupt random= new RandomChooseToCorrupt();
		random.flipRandomBit(sndpkt);

		return sndpkt;
	}
}

class RandomChooseToCorrupt{
	Random rand = new Random();
	

	public void flipRandomBit(byte [] pkt){
		//if a bit should be flipped
		int  n = rand.nextInt(15);//should be 5, 1 ALWAYS guarantees an error, used for testing

		if(n==0){ // flip a bit
			//choose a random byte 
			int m = rand.nextInt(2);//TO CHANGE, FOR TESTING OF SMALL DATA

			//flip random bit of the 8 
			int o = rand.nextInt(7);
			pkt[m] = (byte) (pkt[m] ^ (1 << o));

		}
	}
}

class Rdt_client extends Rdt{
	private byte sum;
	private byte checkSum;
	private int expectedSeq=0;
	private int receivedSeq;	
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;
	private byte[] sendData = new byte[1024];
  	private byte[] receiveData = new byte[1024];

	//rdt_send and rdt_rcv of the client is masked under this function
	public byte[] udt_rcv(DatagramSocket clientSocket,InetAddress IPAddress,int port){


		byte [] toReturn = rdt_rcv(clientSocket,IPAddress,port);

		//since packet of expected seq was received change seq
		if(expectedSeq==1)expectedSeq=0;
		else expectedSeq=1;

		return toReturn;
	}


	public void rdt_send(String ackOrNac,DatagramSocket clientSocket,InetAddress IPAddress,int port){//format: seq,checksum
		byte[] sndpkt;
		if(ackOrNac.equals("ACK")) sndpkt=make_pkt(receivedSeq);
		else sndpkt=make_pkt((receivedSeq+1)%2);

		//sends packet using udp datagram
		sendPacket = new DatagramPacket(sndpkt, sndpkt.length, IPAddress, port);
		try{
			clientSocket.send(sendPacket);
		}catch(Exception e){System.out.println("Exception sending from client to server"+e);}
	}

	//returns what was received as bytes
	public byte[] rdt_rcv(DatagramSocket clientSocket,InetAddress IPAddress,int port){//format: checksum,seq,data
		byte expectedChecksum;
		byte receivedChecksum;
		byte [] rcvpkt;
		byte [] temp;
		byte [] temp2;
		boolean corrupt=false;
		boolean isAck=true;

		//receive packet using udp datagram
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try{
			clientSocket.receive(receivePacket);
		}catch(Exception e){System.out.println("Exception receiving from server to client");}

		rcvpkt=receivePacket.getData();

		//check if not corrupt, using checksum
		expectedChecksum=rcvpkt[0];
		temp=Arrays.copyOfRange(rcvpkt,1,rcvpkt.length); //rcvpkt.length -1 ?
		receivedChecksum=calcChecksum(temp);
		if (receivedChecksum!=expectedChecksum) {
			corrupt=true;
		}

		//check if correct seq from what is expected
		receivedSeq=temp[0] & 0xFF; // converting from byte to int
		if(receivedSeq!=expectedSeq){
			isAck=false;
		}

		if(corrupt || !isAck) {
			if(corrupt){
				System.out.println("\nTRANSFER ERROR: Packet sent from server was corrupted");
			}else if(!isAck){
				System.out.println("\nTRANSFER ERROR: Packet sent from server was not of the right sequence expected");
			}
			
			rdt_send("NAC",clientSocket,IPAddress,port);// send nack
			return rdt_rcv(clientSocket,IPAddress,port);//call the function again until data is received correctly
		}
		else{
			rdt_send("ACK",clientSocket,IPAddress,port);// send ack
			//return data aspect
			temp2=Arrays.copyOfRange(temp,1,temp.length); //rcvpkt.lenght -1 ?
			return temp2;

		}

	}
	public byte[] make_pkt(int seq){ 
		byte [] checksum = new byte[1];
		byte [] a = new byte [1];
		a[0] = (byte)seq;

		//calc sum
		checksum[0]= calcChecksum(a);

		//concat check sum with data and header(seq)
		byte [] sndpkt = concatByteArr(checksum,a);

		RandomChooseToCorrupt random= new RandomChooseToCorrupt();
		random.flipRandomBit(sndpkt);

		return sndpkt;
	}
}

//keeps record of the progress of what data was sent, if data was not sent yet it enters a queue
class ClientNode{
	private String IPAddress;
	private int seq;
	private Queue<byte[]> unsent;// queue of unsent data

	public ClientNode(String IPAddress){
		this.IPAddress=IPAddress;
		this.seq=0; //first seq expected
		this.unsent= new LinkedList<byte[]>();
	}

	//getters and setters

	public String getIPAddress(){
		return this.IPAddress;
	}

	public int getSeq(){
		return this.seq;
	}

	public Queue<byte[]> getUnsent(){
		return this.unsent;
	}

	public void setIPAddress(String IPAddress){
		this.IPAddress=IPAddress;
	}

	public void setSeq(int seq){
		this.seq=seq;
	}
}
