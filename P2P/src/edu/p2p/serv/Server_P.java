package edu.p2p.serv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import edu.p2p.Object.Object;

public class Server_P {

	static String RootFileLocation = "C:\\p2p";
	static String BaseFileLocation = RootFileLocation + "/Server";
	static Map<Integer, ArrayList<Integer>> MapClient;

	ServerSocket SocketReceive;
	Socket SocketConnection;
	private int OwnerServerPort = 21000;
	
	int ChunkSize = 102400;

	public static void main(String[] args) {

		Server_P Owner = new Server_P();
		try {
			
			Owner.DivideFileIntoChunk();//Divide file into Chunks
							
			String Chunk_Location = BaseFileLocation + "/chunks";
			File[] ChunkFiles = new File(Chunk_Location).listFiles();

			int Count_Chunk = ChunkFiles.length;
			System.out.println("Total number of Chunks:" + Count_Chunk);

			MapClient = new LinkedHashMap<Integer, ArrayList<Integer>>();
			for (int i = 1; i <= 5; i++) {
				ArrayList<Integer> list = new ArrayList<Integer>();
				for (int j = i; j <= Count_Chunk; j += 5) {
					list.add(j);
				}
				MapClient.put(i, list);

			}
			System.out.println(MapClient);

			if (Count_Chunk > 0) {				
				Owner.ServerConnection(ChunkFiles);
			} else
				System.out.println("No Chunks in folder!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void ServerConnection(File[] ChunkFiles) {
		try {
			int client = 0; // initialized to 0
			SocketReceive = new ServerSocket(OwnerServerPort);
			System.out.println("Server socket created, waiting for connections...");
			while (true) {
				
				client++;
				if (client <= MapClient.size()) {
					SocketConnection = SocketReceive.accept();
					System.out.println("new connection with Client" + client);
					new Ser_thread(SocketConnection, client, ChunkFiles, MapClient.get(client)).start();

				} else {
					System.out.println("No more clients!");
					break;
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public void DivideFileIntoChunk() {

		try {
			
			Scanner inputname=new Scanner(System.in);
			System.out.println("Enter the file name:");
			String input=inputname.nextLine();

			File InFile = new File(BaseFileLocation +"/"+input);
			Long fileLength = InFile.length();

			System.out.println("The size of input file: " + fileLength);

			String Chunk_directory = InFile.getParent() + "/chunks/";
			File ChunkFolder = new File(Chunk_directory);
			
			
			
			if (ChunkFolder.mkdirs())	
				System.out.println("chunk folder is created");
			else
				System.out.println("Unable to create chunk folder!");

			
			byte[] chunk = new byte[102400];

			FileInputStream InputFileInStream = new FileInputStream(InFile);

			BufferedInputStream bufferStream = new BufferedInputStream(InputFileInStream);
			int index_Chunk = 1;
			int Read_Bytes;

			while ((Read_Bytes = bufferStream.read(chunk)) > 0) {
				FileOutputStream OutFile = new FileOutputStream(
						new File(Chunk_directory, String.format("%04d", index_Chunk) + "_" + InFile.getName()));
				BufferedOutputStream bufferout = new BufferedOutputStream(OutFile);
				bufferout.write(chunk, 0, Read_Bytes);
				bufferout.close();
				System.out.printf("this [%d] chunk has %d size.\n",index_Chunk, Read_Bytes);
				index_Chunk++;
			}
			bufferStream.close();
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class Ser_thread extends Thread {

	private Socket socket;
	ObjectOutputStream outStream;
	int clientnum;
	ArrayList<Integer> chunkList;
	File[] ChunkFiles;

	Ser_thread(Socket Owner, int client, File[] ChunkFiles, ArrayList<Integer> cl) {
		this.socket = Owner;
		this.clientnum = client; 
		this.ChunkFiles = ChunkFiles;
		this.chunkList = cl;

	}

	public void run() {
		try {
			outStream = new ObjectOutputStream(socket.getOutputStream());
		
			outStream.writeObject(ChunkFiles.length);
			outStream.writeObject(chunkList.size());

			Arrays.sort(ChunkFiles);
			for (int i = 0; i < chunkList.size(); i++) {
				Object send_chunk_object = build_chunk(ChunkFiles[chunkList.get(i) - 1], chunkList.get(i));
				send_chunk(send_chunk_object);
				Thread.sleep(1000);
			}
			
			Disconnect();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Object build_chunk(File file, int chunkNum) throws IOException {
		byte[] chunk = new byte[102400];
		System.out.println("construct object - " + file.getName());
		Object Chunk_Object = new Object();

		Chunk_Object.setFileNum(chunkNum);
		Chunk_Object.setFileName(file.getName());
		
		FileInputStream InputFileInStream = new FileInputStream(file);
		BufferedInputStream bufferInStream = new BufferedInputStream(InputFileInStream);

		int Read_Bytes = bufferInStream.read(chunk);

		Chunk_Object.setChunksize(Read_Bytes);
		Chunk_Object.setFileData(chunk);

		bufferInStream.close();
		InputFileInStream.close();

		return Chunk_Object;
	}

	public void send_chunk(Object send_chunk_object) {
		try {

			outStream.writeObject(send_chunk_object);
			outStream.flush();
			
			System.out.println("send chunk to Client" + clientnum);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void Disconnect() {
		try {
			outStream.close();
			socket.close();
			System.out.println("Server socket closed with Client" + clientnum);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
