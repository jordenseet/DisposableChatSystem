package aa.ClientServer;
import aa.Entities;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	private int port;
	private static int connectionID;
	private ArrayList<ClientThread> clients; //keeps list of clients
	private boolean running;
	
	public Server(int port) {
		this.port = port;
		clients = new ArrayList<ClientThread>();
	}

	public void start() {
		running = true;
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while(running) 
			{
				System.out.println("Server waiting for potential clients on port " + port);
				
				// accept connection if a client submits a request
				Socket socket = serverSocket.accept();

				if(!running){
					break;
				}

				//Each client has to be on a thread to run concurrently as one port can only stream one line of communication
				ClientThread t = new ClientThread(socket);
				clients.add(t);
				
				t.start();
			}

			//if not running
			try {
				serverSocket.close();
				for(int i = 0; i < clients.size(); ++i) {
					ClientThread t = clients.get(i);
					try {
						t.input.close();
						t.output.close();
						t.socket.close();
					}
					catch(Exception e) {
						System.out.println("Exception occured while closing streams and sockets: " + e);
					}
				}
			}
			catch(Exception e) {
				System.out.println("Exception occured while closing the server and clients: " + e);
			}
		}
		catch (Exception e) {
			System.out.println("Exception occured while trying to start server: " + e);
		}
	}

	protected void stop() {
		running = false;
	}
	
	private synchronized boolean broadcast(String message) {
		
		// to check if message is private to a specific client
		String[] w = message.split(" ",3);
		
		boolean privateMsg = false;

		if(w[1].charAt(0)=='@'){
			privateMsg=true;
		}
		
		
		// if private message, send message to specified client
		if(privateMsg==true){
			String toCheck=w[1].substring(1, w[1].length());
			message=w[0]+w[2];
			boolean found=false;

			// naive bruteforce search for client name
			for(int i=0;i<clients.size();i++){
				ClientThread currentClient=clients.get(i);

				if(currentClient.getUsername().equals(toCheck)){
					//calling send executes the method and returns a boolean of whether the message went through
					//so we just compare the returned value as the method already attempts the message sending

					if(!currentClient.send(message)) {
						clients.remove(i);
						System.out.println(currentClient.username + " has been disconnected and message could not be sent.");
					}

					else{
						found=true;
						break; //exits the searching loop
					}

				}
			}
			if(!found){
				return false; 
			}
		}
		// if message is a broadcast message
		else{
			System.out.println(message);
			// naive bruteforce search for client name
			// searches from reverse to prevent indexing issues when removing disconnected clients
			for(int i=clients.size()-1;i>=0;i--) {
				ClientThread currentClient = clients.get(i);
				// if message cannot be sent, it means client has disconnected
				// thus, we clean up the currently connected client list
				if(!currentClient.send(message)) {
					clients.remove(i);
					System.out.println(currentClient.username + " has been disconnected and message could not be sent.");
				}
			}
		}
		return true;
	}

	// if client logouts
	synchronized void logoff(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < clients.size(); i++) {
			ClientThread currentClient = clients.get(i);
			// if found remove it
			if(currentClient.id == id) {
				System.out.println(currentClient.getUsername() + " has left the chat room.");
				clients.remove(i);
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		// start server on port 8561 unless a PortNumber is specified 
		int portNumber;
		try {
			portNumber = Integer.parseInt(args[0]);
		}
		catch(Exception e) {
			portNumber = 8561;
		}
		Server server = new Server(portNumber);
		server.start();
	}

	class ClientThread extends Thread {
		Socket socket;
		ObjectInputStream input;
		ObjectOutputStream output;
		int id;
		String username; //just for personalisation purposes
		Message m;

		ClientThread(Socket socket) {
			id = connectionID++; //++ guarantees unique ids
			this.socket = socket;
			System.out.println("Thread " + id + " trying to start object streams");
			try{
				output = new ObjectOutputStream(socket.getOutputStream());
				input  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) input.readObject();
				System.out.println(username + " has joined the chat room.");
			}
			catch (IOException e) {
				System.out.println("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public String getUsername() {
			return username;
		}

		private boolean send(String msg) {
			//cleanup if socket isn't connected
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// send message via object output stream
			try {
				output.writeObject(msg);
			}
			catch(IOException e) {
				System.out.println("Error sending message to " + username + ": " + e);
			}
			return true;
		}

		//close everything for cleanup
		private void close() {
			try {
				if(output != null) output.close();
			}
			catch(Exception e) {}
			try {
				if(input != null) input.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		@Override
		public void run() {
			boolean running = true;

			while(running) {
				//captures the object in the objectinputstream as a message
				try {
					m = (Message) input.readObject();
				}
				catch (IOException e) {
					System.out.println("Exception reading message from " + getUsername() + ": " + e);
					break;				
				}
				catch(Exception ee) {
					ee.printStackTrace();
					break;
				}
				// get the message from the ChatMessage object received
				String message = m.getMessage();

				// different actions based on message type
				
				if (m.getType() == 0){
					//sends currently active clients
					for(int i = 0; i < clients.size(); ++i) {
						ClientThread client = clients.get(i);
						send((i+1) + ") " + client.username);
					}
				}
				else if(m.getType() == 1){
					boolean sent =  broadcast(username + ": " + message);
					if(sent == false){
						String msg ="User does not exist of has logged off.";
						send(msg);
					}
				}
				else if(m.getType() == 2){
					running = false;
					logoff(id);
				}
				else{
					System.out.println("Unsupported message type, please contact the System Administrator");
				}
			}
			close();
		}
	}
}

