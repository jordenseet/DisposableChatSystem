package aa.ClientServer;
import aa.Entities.Message;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
// import java.util.concurrent.Semaphore;

public class Server {
	// for client-server models, we start the server first
	// the server listens on a port for incoming communications from clients
	// this is the port the server will listen to 
	private int port;
	// Each client-server communication will be initiated by the client
	// Each client will use a thread to communicate with the server for concurrency
	// Hence, we keep a list of InitiatorThreads that represents each client
	private ArrayList<InitiatorThreads> clients;
	// Lock used to prevent race conditions in shared resource
	// In this application, it is limited to the clients arraylist as there are no other shared resource
	private ReentrantLock lock;
	//to keep the server running. When user logs off, running = false
	private boolean running;

	//for experimental purporses
	//private Semaphore sema;
	
	Server(int port) {
		// instantiates port with a given portnumber
		// you can define it using java Server <portnumber>
		// otherwise, default portnumber 8561 will be used
		this.port = port;
		// creates a new arraylist of clients
		clients = new ArrayList<InitiatorThreads>();
		// creates a new lock used for this server to prevent race conditions used via initiatorthreads
		// which are threads initiated by individual clients when attempting to communicate with the this server
		lock = new ReentrantLock();
		//sema = new Semaphore(2); //Allowing two threads to access the same shared resource, in my case, allows race conditions.
	}

	public void start() {
		// begin server running
		running = true;
		try {
			//create new server socket with desired port number
			ServerSocket serverSocket = new ServerSocket(port);
			while(running) {
				System.out.println("To start with a specific port, please use 'java Server <portnumber>'");
				System.out.println("Server waiting for potential clients on port " + port);
				
				// accept connection if a client submits a request
				Socket socket = serverSocket.accept();

				// if at any point of time running is false (interrupted or logged out), we exit the while loop
				if(!running){
					break;
				}

				// Each client has to be on a thread to run concurrently as one port can only stream one line of communication
				InitiatorThreads t = new InitiatorThreads(socket);
				clients.add(t);
				
				// call start method of the thread
				t.start();
			}

			//if not running anymore
			try {
				// close serversocket
				serverSocket.close();

				// since server is shutdown, close the communication streams with client threads
				for(int i = 0; i < clients.size(); ++i) {
					InitiatorThreads t = clients.get(i);
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
	
	private boolean broadcast(String message) {	

		// to check if message is private to a specific client
		// messages have format @username<space>yourmessage
		// need to split to a maximum of 3 positions to preserve the message

		// full message comprises of
		// [0] => the sender's name
		// [1] => sending to who
		// [2] => the message in whole
		String[] fullmessage = message.split(" ",3);
		

		boolean privateMsg = false;

		//only if there is an @ in the w[1], which would take in the username as per the format, will it be a private msg
		if(fullmessage[1].charAt(0)=='@'){
			privateMsg=true;
		}
		
		
		// if private message, send message to specified client
		if(privateMsg==true){
			//retrieving username to check later
			String toCheck=fullmessage[1].substring(1, fullmessage[1].length());
			//concatenate strings to form the message, both from whom and the message
			message=fullmessage[0]+fullmessage[2];

			boolean found=false;

			// naive bruteforce search for client name
			//lock clients arraylist down to prevent race condition
			lock.lock();
			//sema.acquire();
			try{
				for(int i=0;i<clients.size();i++){
					InitiatorThreads currentClient=clients.get(i);

					if(currentClient.getUniqueId().equals(toCheck)){

						//calling send executes the method and returns a boolean of whether the message went through
						//so we just compare the returned value as the method already attempts the message sending
						if(!currentClient.send(message)) {
							// just cleaning up if client cannot be found
							clients.remove(i);
							System.out.println(currentClient.uniqueId + " has been disconnected and message could not be sent.");
						}

						else{
							found=true;
							break; //exits the searching loop
						}

					}
				}				
			}
			finally{
				lock.unlock();
				//sema.release();
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
			//lock clients arraylist down to prevent race condition
			lock.lock();
			//sema.acquire();

			try{
				for(int i=clients.size()-1;i>=0;i--) {
					InitiatorThreads currentClient = clients.get(i);
					// if message cannot be sent, it means client has disconnected
					// thus, we clean up the currently connected client list
					if(!currentClient.send(message)) {
						// just cleaning up if cannot find client
						clients.remove(i);
						System.out.println(currentClient.uniqueId + " has been disconnected and message could not be sent.");
					}
				}				
			}
			finally{
				lock.unlock();
				//sema.release();
			}
		}
		return true;
	}

	// if client logouts
	void logoff(int id) {
		// scan the array list until we found the id
		lock.lock();
		//sema.acquire();
		try{
			for(int i = 0; i < clients.size(); i++) {
				InitiatorThreads currentClient = clients.get(i);
				// if found remove it
				if(currentClient.id == id) {
					System.out.println(currentClient.getUniqueId() + " has left the chat room.");
					clients.remove(i);
					break;
				}
			}			
		}
		finally{
			lock.unlock();
			//sema.release();
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

	// As mentioned, each client will be represented by a thread
	// It is called an initiator thread because this thread is formed when a client initiates communication with this thread

	class InitiatorThreads extends Thread {
		Socket socket;
		ObjectInputStream input;
		ObjectOutputStream output;

		int id;
		String uniqueId;
		Message m;

		InitiatorThreads(Socket socket) throws Exception {

			// instantiates socket
			this.socket = socket;
			try{
				// object input/output streams are streams of communication between the client and the server
				// this allows objects, such as messages, to be transferred 
				output = new ObjectOutputStream(socket.getOutputStream());
				input  = new ObjectInputStream(socket.getInputStream());

				// read the uniqueId
				uniqueId = (String) input.readObject();
			}
			catch (IOException e) {
				System.out.println("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (Exception e) {
				System.out.println(e);
			}
		}
		
		public String getUniqueId() {
			return uniqueId;
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
				System.out.println("Error sending message to " + getUniqueId() + ": " + e);
			}
			return true;
		}

		//close everything for cleanup
		private void close() {
			try { 
				if(input != null) {
					input.close();
				}
			}
			catch(Exception e) {
				System.out.println("Error closing inputstream");
			}
			try {
				if(output != null){
					output.close();
				}
			}
			catch(Exception e) {
				System.out.println("Error closing outputstream");
			}
			try{
				if(socket != null){
					socket.close();
				}
			}
			catch(Exception e) {
				System.out.println("Error closing socket");
			}
				
		}

		@Override
		public void run() {

			// begins running of server
			boolean running = true;

			while(running) {

				//captures the object in the objectinputstream as a message
				try {
					// reads the message as an object
					// that is why we used objectInputStream
					m = (Message) input.readObject();
				}
				catch (IOException e) {
					System.out.println("Exception reading message from " + getUniqueId() + ": " + e);
					break;				
				}
				catch(Exception ee) {
					ee.printStackTrace();
					break;
				}
				// get the message from the Message object received
				String message = m.getMessage();

				// different actions based on message type
				if (m.getType() == 0){
					// this checks who are the people currently active in the chat
					for(int i = 0; i < clients.size(); ++i) {
						InitiatorThreads client = clients.get(i);
						// gets the client's uniqueId 
						send((i+1) + ") " + client.getUniqueId());
					}
				}
				else if(m.getType() == 1){
					// this options indicate sending of messages
					boolean sent =  broadcast(uniqueId + ": " + message);
					if(sent == false){
						String msg ="User does not exist of has logged off.";
						send(msg);
					}
				}
				else if(m.getType() == 2){
					// this message indicates logging off
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

