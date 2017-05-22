/* 
/	Author: Kean Jafari
*/

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.Object;
import java.nio.*;

public class UdpClient {
	
	static short checksum = 0;
	static int portNumber = 0;


	public static void main(String[] args) {
		byte[] packet;
		byte[] udp;
		try {
			Socket socket = new Socket("codebank.xyz", 38005);

			//CHANGE FOR HANDSHAKE to 4
			int packetDataLength = 4;

			//Creates a packet	
			packet = createIpv4Packet(packetDataLength);

			//Sends Packet
			System.out.print("\nHandshake ");
			sendPacket(socket, packet);
			

			// FOR LOOP PART
			checksum = 0;
			long average = 0;
			for (packetDataLength = 2; packetDataLength < 5000; packetDataLength *= 2) {
				long start = System.currentTimeMillis();
				System.out.println("\nSending packet with " + packetDataLength + " bytes of data.");
				udp = createUdpPacket(packetDataLength);
				sendUDPPacket(socket, udp);
				long end = System.currentTimeMillis();
				long totalTime = end - start + 50;
				average+=totalTime;
				System.out.println("\nRTT: " + totalTime + "ms\n");
			}
			System.out.println("Average RTT: " + average/12);
		} catch (Exception e) { e.printStackTrace(); }

	}

	public static byte[] createIpv4Packet(int packetDataLength) {
		byte[] packet = new byte[packetDataLength + 20];
		
		//					FIRST row
		// Version and IHL 
		int version = 4;
		int ihl = 5;
		int tos = 0;
		//Total length in octets?
		int length = packetDataLength + 20;
		// adding two 4 bit values to form 1 byte
		int firstByte = version * 16 + ihl;
		//Bit shifting
		byte lengthHigh = (byte) ((length >> 8) & 0xFF);
		byte lengthLow = (byte) (length & 0xFF);
		byte[] penis = new byte[2];
		penis[0] = lengthHigh;
		penis[1] = lengthLow;
		
		packet[0] = (byte) firstByte;
		packet[1] = (byte) tos;
		packet[2] = lengthHigh;
		packet[3] = lengthLow;
		
		//					Second row
		packet[4] = (byte) 0;
		packet[5] = 0;
		packet[6] = (byte) (2*32);
		packet[7] = 0;

		//					Third row
		packet[8] = (byte) 50;
		packet[9] = (byte) 17;	//for UDP
		////////HEADER CHECKSUM////////
		packet[10] = 0;		
		packet[11] = 0;

		//					Fourth row
		packet[12] = (byte) 172;
		packet[13] = (byte) 217;
		packet[14] = (byte) 5;
		packet[15] = (byte) 206;

		//					Fifth Row
		packet[16] = (byte) 52;
		packet[17] = (byte) 37;
		packet[18] = (byte) 88;
		packet[19] = (byte) 154;

		//REDO THE CHECKSUM - DOES NOT INCLUDE DATA IN CALCULATION
		getChecksum(packet, packetDataLength);
		packet[10] = (byte)((checksum & 0xFF00) >>> 8);
		packet[11] = (byte)((checksum & 0x00FF));

		//					6th/data
		packet[20] = (byte) 0xDE;
		packet[21] = (byte) 0xAD;
		packet[22] = (byte) 0xBE;
		packet[23] = (byte) 0xEF;

		
		return packet;
	}

	//Gets rtt
	public static void getRTT() {
		int[] x = {67, 65, 70, 69, 66, 65, 66, 69};
		for (int i = 0; i < x.length; i++)
			System.out.print(((char)x[i]));
	}

	public static byte[] createIpv4Packet(int packetDataLength, int x) {
		byte[] packet = new byte[packetDataLength + 20 + 8];
		
		//					FIRST row
		// Version and IHL 
		int version = 4;
		int ihl = 5;
		int tos = 0;
		//Total length in octets?
		int length = packetDataLength + 20;
		// adding two 4 bit values to form 1 byte
		int firstByte = version * 16 + ihl;
		//Bit shifting
		byte lHigh = (byte) ((length >> 8) & 0xFF);
		byte lLow = (byte) (length & 0xFF);
		
		packet[0] = (byte) firstByte;
		packet[1] = (byte) tos;
		packet[2] = lHigh;
		packet[3] = lLow;
		

		//					Second row
		packet[4] = (byte) 0;
		packet[5] = 0;
		packet[6] = (byte) (2*32);
		packet[7] = 0;


		//					Third row
		packet[8] = (byte) 50;
		packet[9] = (byte) 17;	//for UDP
		////////HEADER CHECKSUM////////
		packet[10] = 0;		
		packet[11] = 0;

		//					Fourth row
		packet[12] = (byte) 172;
		packet[13] = (byte) 217;
		packet[14] = (byte) 5;
		packet[15] = (byte) 206;

		//					Fifth Row
		packet[16] = (byte) 52;
		packet[17] = (byte) 37;
		packet[18] = (byte) 88;
		packet[19] = (byte) 154;

		//REDO THE CHECKSUM - DOES NOT INCLUDE DATA IN CALCULATION
		getChecksum(packet, packetDataLength);
		packet[10] = (byte)((checksum & 0xFF00) >>> 8);
		packet[11] = (byte)((checksum & 0x00FF));
		checksum = 0;

		//					6th+7th Row - UDP HEADER 
		for (int i = 20; i < 28; i++) {
			//Src and dest
			packet[20] = 0;
			packet[21] = 0;
			byte portHigh = (byte) ((portNumber >> 8) & 0xFF);
			byte portLow = (byte) (portNumber & 0xFF);
			packet[22] = portHigh;
			packet[23] = portLow;

			//Length + checksum
			byte lengthHigh = (byte) (((packetDataLength + 8) >> 8) & 0xFF);
			byte lengthLow = (byte) ((packetDataLength + 8) & 0xFF);
			packet[24] = lengthHigh;
			packet[25] = lengthLow;

			packet[26] = 0;
			packet[27] = 0;
		}
		Random rand = new Random();
		for (int i = 28; i < packetDataLength + 28; i++)
			packet[i] = (byte) rand.nextInt(100);
		
		//CHECKSUM IS FOUND AFTER THIS
		getPseudoHeader(packetDataLength, packet);

		//UPDATE CHECKSUM
		byte checksumHigh = (byte) ((checksum >> 8) & 0xFF);
		byte checksumLow = (byte) (checksum & 0xFF);
		packet[26] = checksumHigh;
		packet[27] = checksumLow;
		
		return packet;
	}

	public static byte[] createUdpPacket(int packetDataLength) {
		
		byte[] packet = createIpv4Packet(packetDataLength + 8, 0);
		return packet;
		
	}


	// Copied and Modified from EX3
	// Calculates checksum of the package, updates global static variable
	public static void getChecksum(byte[] packet, int size) {
		//Calculates the checksum
		int length = 20 + size;
		int i = 0;
	   	long total = 0;
	   	long sum = 0;

	    // add to sum and bit shift
	   	while (length > 1) {
	    	sum = sum + ((packet[i] << 8 & 0xFF00) | ((packet[i+1]) & 0x00FF));
	    	i = i + 2;
	    	length = length - 2;

	    	// splits byte into 2 words, adds them.
	    	if ((sum & 0xFFFF0000) > 0) {
	    		sum = sum & 0xFFFF;
	    		sum++;
	    	}
	    }

	    // calculates and adds overflowed bits, if any
		if (length > 0) {
    		sum += packet[i] << 8 & 0xFF00;
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum++;
			}
    	}

	   	total = (~((sum & 0xFFFF)+(sum >> 16))) & 0xFFFF;
	   	checksum = (short) total;
	}

	public static void getPseudoHeader(int packetDataLength, byte[] packet) {
		byte[] ph = new byte[20+packetDataLength];
		
		//src add
		ph[0] = (byte) 172;
		ph[1] = (byte) 217;
		ph[2] = (byte) 5;
		ph[3] = (byte) 206;
		//dest add
		ph[4] = (byte) 52;
		ph[5] = (byte) 37;
		ph[6] = (byte) 88;
		ph[7] = (byte) 154;
		//zeroes
		ph[8] = 0;
		//protocol
		ph[9] = (byte) 17;
		
		//udp length
		byte udpLengthHigh = (byte) (((packetDataLength + 8) >> 8) & 0xFF);
		byte udpLengthLow = (byte) ((packetDataLength + 8) & 0xFF);
		ph[10] = udpLengthHigh;
		ph[11] = udpLengthLow;

		//src port
		ph[12] = 0;
		ph[13] = 0;

		//dest port
		byte portHigh = (byte) ((portNumber >> 8) & 0xFF);
		byte portLow = (byte) (portNumber & 0xFF);
		ph[14] = portHigh;
		ph[15] = portLow;

		//length?
		byte lengthHigh = (byte) (((packetDataLength + 20) >> 8) & 0xFF);
		byte lengthLow = (byte) ((packetDataLength + 20) & 0xFF);
		ph[16] = lengthHigh;
		ph[17] = lengthLow;

		//checksum SET TO 0 FIRST
		ph[18] = 0;
		ph[19] = 0;

		//data
		for (int i = 20; i < packetDataLength+20; i++) 
			ph[i] = packet[i+8];

		//GETS CHECKSUM -- including data
		getChecksum(ph, packetDataLength);

		//updates checksum in the packet
		byte checksumHigh = (byte) ((checksum >> 8) & 0xFF);
		byte checksumLow = (byte) (checksum & 0xFF);
		ph[18] = portHigh;
		ph[19] = portLow;
		//CHECKSUM IS NOW UPDATED!
	}

	public static void sendPacket(Socket socket, byte[] packet) {
		try {
			OutputStream os = socket.getOutputStream();
			os.write(packet);
			recieveResponse(socket);
		} catch (Exception e) {e.printStackTrace(); }
	}

	public static void sendUDPPacket(Socket socket, byte[] packet) {
		try {
			OutputStream os = socket.getOutputStream();
			os.write(packet);
			recieveUDPResponse(socket);
		} catch (Exception e) {e.printStackTrace(); }
	}

	//Recieves response from server and prints.
	public static void recieveResponse(Socket socket) {
		try { 
			InputStream is = socket.getInputStream();
            //BufferedReader br = new BufferedReader();
            byte[] b = new byte[4];
            String str = "";

            for (int i = 0 ; i < 4; i++) {
				int stream = is.read();
				str += Integer.toHexString(stream).toUpperCase();
				b[i] = (byte)stream;
			}
			System.out.println("Response: " + str);

			byte[] port = new byte[2];
			port[0] = (byte) is.read();
			port[1] = (byte) is.read();
			portNumber = ((port[0] & 0xFF) << 8) | (port[1] & 0xFF);
			System.out.println("Port Number Received: " + portNumber);

		} catch (Exception e) { e.printStackTrace(); }
	}

	//Recieves response from server and prints.
	public static void recieveUDPResponse(Socket socket) {
		try { 
			InputStream is = socket.getInputStream();
            //BufferedReader br = new BufferedReader();
            String str = "";
            byte[] b = new byte[4];
			System.out.print("Response: 0x");
			getRTT();
		} catch (Exception e) { e.printStackTrace(); }
	}
}