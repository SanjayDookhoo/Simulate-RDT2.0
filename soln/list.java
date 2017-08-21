import java.net.*;
import java.io.*;
import java.util.*;

public class list{
    public static void main(String args[]){
		Queue<byte[]> q = new LinkedList<byte[]>();
		byte[] data = new byte[1024];
		String s;
		
		//string to byte to store
		s="sanjay";
		data=s.getBytes();
		q.add(data);

		//string to byte to store
		s="dookhoo";
		data=s.getBytes();
		q.add(data);
		
		//convert byte to string then print
		data=q.remove();
		s=new String(data);
		System.out.println(s);

		//convert byte to string then print
		data=q.remove();
		s=new String(data);
		System.out.println(s);

		try{
			data=q.remove();
		}catch(Exception e) {}
    }
}

