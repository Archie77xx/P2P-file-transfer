package edu.p2p.cli2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.p2p.Object.Object;

public class Client_P2 {
	
	ServerSocket SocketReceive;
	Socket SocketConnection;
	Socket SocketClient;
	private int FileOwnerPort=21000;
	private int ServerPort=21002;
	private int ClientPort=21003;
	static String RootFileLocation = "C:\\p2p";
	static String BaseFileLocation = RootFileLocation + "/ClientP2";
	static String chunksLocation = BaseFileLocation + "/chunks";

	ObjectInputStream inStream;
	ObjectOutputStream outStream;
	Set<Integer> chunkList;


	public static void main(String[] args) {
		Client_P2 Client = new Client_P2();
		
		new File(BaseFileLocation + "/chunks/").mkdirs();
				
		int TotalRecFiles;
		try {
			
			Client.CreatConnection(Client.FileOwnerPort);
			TotalRecFiles = (int) Client.inStream.readObject();
			Client.chunkList = Collections.synchronizedSet(new LinkedHashSet<Integer>());
			
			for (int i = 1; i <= TotalRecFiles; i++)
				Client.chunkList.add(i); 

			int filesToReceive = (int) Client.inStream.readObject();
			
			while (filesToReceive > 0) {
				Object receive_chunk_object = Client.ChunkReceive();
				if (receive_chunk_object != null)
					Client.CreateChunkToSend(chunksLocation, receive_chunk_object);
				else
					System.out.println("No chunk to receive!");
				filesToReceive--;
			}
			Client.ClientDisconnect();

		Thread thread =new Thread(new Runnable(){
			public void run(){
				Client.Connect_Client(Client.ServerPort);			
			}
		}
		);
		
		thread.start();
		
		Client.CreatConnection(Client.ClientPort); 
		
		while(true)
		{
			//System.out.println("List for Client 2 to download:" + Client.chunkList);
			
			if(!Client.chunkList.isEmpty())
			{
				Integer[] array = Client.chunkList.toArray(new Integer[Client.chunkList.size()]);
				List<Integer> listregister = new ArrayList<>();
				for(int i=0;i<array.length;i++)
				{ 					
					listregister.add(array[i]);
				}
				System.out.println(listregister);
				for(int i=0;i<array.length;i++)
				{
				if(!listregister.isEmpty())
				{
				Random ran = new Random();
				int RandomNumber = ran.nextInt(listregister.size());
				Client.outStream.writeObject(listregister.get(RandomNumber));
				Client.outStream.flush();
				System.out.println("["+Client.ServerPort+"]"+"sends request [" + listregister.get(RandomNumber) + "] to " + "["+ Client.SocketClient.getPort()+"]" );//*new
				listregister.remove(RandomNumber);
				
				Object receive_chunk_object = Client.ChunkReceive();
				if (receive_chunk_object != null)
					Client.CreateChunkToSend(chunksLocation, receive_chunk_object);
				}
				}
			}
			else
			{

				Client.outStream.writeObject(-1);
				Client.outStream.flush();
				break;
			}
			Thread.sleep(2000);
		}
		
		Client.ClientDisconnect();
		
		Client.CombineChunkToFile();
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//* As a server, let client5 connect to
	public void Connect_Client( int port) {
		try {
			int neighbour_c = 1; 
			SocketReceive = new ServerSocket(port);
			System.out.println("Client 2 Server socket created");
			while (true) {
				if (neighbour_c > 0) {
					neighbour_c--;
					SocketConnection = SocketReceive.accept();
					System.out.println("Client 2 connect with " + SocketConnection);

					new OwnerClientThread(SocketConnection, chunksLocation).start();

				} else {
					System.out.println("No more clients!");
					break;
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//*As client to connect to owner and server(client2) 
	public void CreatConnection(int port) throws InterruptedException {
		boolean b=true;
		while(b)
		{
		try {

			b=false;
			SocketClient = new Socket("127.0.0.1", port);
			System.out.println("Client 2 connected to : " + "["+SocketClient.getPort()+"]");
			inStream = new ObjectInputStream(SocketClient.getInputStream());
			outStream = new ObjectOutputStream(SocketClient.getOutputStream());
		}
		catch(ConnectException e)
		{

			System.out.println("Cannot connect to socket "+port+", try it again");
		    Thread.sleep(5000);
			b=true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		}
	}

	public Object ChunkReceive() {
		Object Object_Chunk = null;
		try {
			Object_Chunk = (Object) inStream.readObject();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return Object_Chunk;
	}

	public void ClientDisconnect() {
		try {
			inStream.close();
			SocketClient.close();
			System.out.println("Client 2 connection closed: "+"["+SocketClient.getPort()+"]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void CreateChunkToSend(String chunksLocation, Object receive_chunk_object) {
		try {
			System.out.println("Received chunk " + receive_chunk_object.getFileName() + " from ["+SocketClient.getPort()+"]");
			FileOutputStream OutFile = new FileOutputStream(
					new File(chunksLocation, receive_chunk_object.getFileName()));
			BufferedOutputStream bufferOut = new BufferedOutputStream(OutFile);
			bufferOut.write(receive_chunk_object.getFileData(), 0, receive_chunk_object.getChunksize());

			chunkList.remove(receive_chunk_object.getFileNum());

			bufferOut.flush();
			bufferOut.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void CombineChunkToFile() {

		String chunksLocation = BaseFileLocation + "/chunks";
		File[] files = new File(chunksLocation).listFiles();
		byte[] chunk = new byte[102400]; 
		
		new File(BaseFileLocation + "/out/").mkdirs();
		
		try {

			FileOutputStream OutFile = new FileOutputStream(
					new File(BaseFileLocation + "/out/"+ files[0].getName()));
			BufferedOutputStream bufferOut = new BufferedOutputStream(OutFile);
			for (File f : files) {
				FileInputStream fileInStream = new FileInputStream(f);
				BufferedInputStream bufferInStream = new BufferedInputStream(fileInStream);
				int Read_Bytes = 0;
				while ((Read_Bytes = bufferInStream.read(chunk)) > 0) {
					bufferOut.write(chunk, 0, Read_Bytes);
				}
				fileInStream.close();
			}
			OutFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class OwnerClientThread extends Thread {

	private Socket socket;
	ObjectOutputStream outStream;
	ObjectInputStream inStream;
	String ChunkLocation;

	OwnerClientThread(Socket s, String ChunkLocation){
		this.socket = s;
		this.ChunkLocation = ChunkLocation;
	}

	public void run() {
		try {

			outStream = new ObjectOutputStream(socket.getOutputStream());
			inStream = new ObjectInputStream(socket.getInputStream());

			while(true)
			{
			int ChunkTotalNumber= (int)inStream.readObject();
			if(ChunkTotalNumber<0)
				break;
			File[] files = new File(ChunkLocation).listFiles();
			String[] s;
			File CurrentChunk=null;
			boolean FileExist=false;
			for(int i=0;i< files.length;i++)
			{
				CurrentChunk=files[i];
				s=files[i].getName().split("_");
				if(ChunkTotalNumber == Integer.parseInt(s[0]))
				{
					FileExist=true;
					break;		
				}
			}
			Object send_chunk_object;
			if(FileExist)
			{
				System.out.println("["+socket.getLocalPort()+"]"+" Receives request [" + ChunkTotalNumber + "] from [21001], accepted." );//*new
				send_chunk_object = build_chunk(CurrentChunk, ChunkTotalNumber);
			}
			else
			{
				System.out.println("["+socket.getLocalPort()+"]"+" Receives request [" + ChunkTotalNumber + "] from [21001], not accepted." );//*new
				send_chunk_object = build_chunk(null, -1);
			}
				sendObject(send_chunk_object);
				System.out.println("["+socket.getLocalPort()+"]"+ " Request [list to send] from [21001]");
			}		
			ServerDisconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized Object build_chunk(File file, int chunkNum) throws IOException {
		Object Object_Chunk = null;
		if(chunkNum>0)
		{
			byte[] chunk = new byte[102400];
			Object_Chunk = new Object();
			System.out.println("["+socket.getLocalPort()+"]"+" Sends chunk " + file.getName() + " to " + "[21001]" );
	
			Object_Chunk.setFileNum(chunkNum);
	
			Object_Chunk.setFileName(file.getName());
			FileInputStream fileInStream = new FileInputStream(file);
	
			BufferedInputStream bufferInStream = new BufferedInputStream(fileInStream);
	
			int Read_Bytes = bufferInStream.read(chunk);
	
			Object_Chunk.setChunksize(Read_Bytes);
	
			Object_Chunk.setFileData(chunk);
	
			bufferInStream.close();
			fileInStream.close();
		}
		return Object_Chunk;
	}

	public void sendObject(Object send_chunk_object) {
		try {
			outStream.writeObject(send_chunk_object);
			outStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void ServerDisconnect() {
		try {
			outStream.close();
			socket.close();
			System.out.println("Client 2 - server socket closed : " + "[21001]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
