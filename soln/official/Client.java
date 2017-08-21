/*
client receives data in some format and renders 
client determines how to render the different data 
*/

import java.net.*;
import java.io.*;
import java.util.*;

public class Client{
    public static void main(String args[])  throws Exception{
		// args give message contents and destination hostname
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
			int port=9876;
			try{
				InetAddress IPAddress = InetAddress.getByName("localhost");
				Render render = new Render(clientSocket,IPAddress,port);

				//establish connection
				byte[] sendData  = new byte[1024];
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);

				while(true){
					render.stringRender();
				}
			}catch (Exception e){System.out.println("Exception assigning address");}
		}catch (Exception e){System.out.println("Exception assigning socket");}
	}
}

class Render {
	private DatagramSocket clientSocket;
	private InetAddress IPAddress;
	private int port;

	//data to receive
	Rdt_client rdt_client = new Rdt_client();

	public void stringRender(){
		System.out.println("\tReceived: " + (new String(rdt_client.udt_rcv(clientSocket,IPAddress,port))).trim());
	}

	public Render(DatagramSocket clientSocket,InetAddress IPAddress,int port){
		this.clientSocket=clientSocket;
		this.IPAddress=IPAddress;
		this.port=port;
	}
}
