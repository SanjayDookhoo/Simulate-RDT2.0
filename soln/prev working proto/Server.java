/*
Server sends data from some format to be rendered by client
server deliberately randomizes which packets to corrupt
server outputs packet sent and packets not sent 

server terminates to connection on its own after x amount of packets is sent 
	the difference in what was not sent properly due to lack of reliability can be clearly seen
*/

import java.net.*;
import java.io.*;
import java.util.*;

class Server {
  public static void main(String args[]) throws Exception
    {
    	System.out.println("UDP Server Started");

		DatagramSocket serverSocket = new DatagramSocket(9876);
		DatagramPacket receivePacket;
		InetAddress IPAddress;
		DatagramPacket sendPacket;
		byte[] receiveData = new byte[1024];
		byte[] sendData  = new byte[1024];
 		List<ClientNode> clients = new ArrayList<ClientNode>();
		
		//gain address and ip of client from first msg received
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		serverSocket.receive(receivePacket);
		System.out.println("Connection established with: "+receivePacket.getAddress().toString()); // testing conenction established
		ClientNode clientNode = new ClientNode(receivePacket.getAddress().toString());

		clients.add(clientNode); // add a client with the related ip address

		//socket data needed
		IPAddress = receivePacket.getAddress();
		int port = receivePacket.getPort();

		//data to send
		DataFormatToSend dataFormatToSend = new DataFormatToSend();
		Rdt_server rdt_server= new Rdt_server();

		while(!dataFormatToSend.stringSendIsEmpty()){
			System.out.println("Sending data");
			rdt_server.udp_send(clients.get(0),dataFormatToSend.stringSend(),
				serverSocket,IPAddress,port);
		}

	}
}

//every function call returns the next data that should be sent (udp_send)
class DataFormatToSend{
	private Queue<String> stringQ;
	

	public boolean stringSendIsEmpty(){
		return stringQ.isEmpty();
	}

	public byte[] stringSend(){
		try{
			return stringQ.remove().getBytes();
		}catch(Exception e) {}
		return new byte[1]; //return anything
	}

	public DataFormatToSend(){
		stringQ = new LinkedList<String>();
		
		stringQ.add("this was a difficult project");
		stringQ.add("i was very confused         ");
		stringQ.add("when it didnt work i cried  ");
		stringQ.add("when it worked i also cried ");
		stringQ.add("can i have full marks please");
		stringQ.add("this will be the best       ");
		stringQ.add("presentation of course      ");
	}
}