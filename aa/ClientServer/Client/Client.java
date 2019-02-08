package aa.ClientServer;
import java.net.*;
import java.io.*;
import java.util.*;


public class Client  {

	private ObjectInputStream input;
	private ObjectOutputStream output;
	private Socket socket;
	
	private String server;
	private String username;
	private int port;

	public String getUsername() {
		return username;
	}
	
	Client(String server, int port, String username) {
		//every client needs to connect to a server
		this.server = server;
		this.port = port;
		this.username = username;
	}
	
	public boolean start() {
		try {
			socket = new Socket(server, port);
		} 
		catch(Exception e) {
			System.out.println("Error connecting to server:" + e);
			return false;
		}
		
		System.out.println("Connection accepted by " + socket.getInetAddress() + ":" + socket.getPort());

		try{
			input  = new ObjectInputStream(socket.getInputStream());
			output = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException e) {
			System.out.println("Exception creating new Input/output Streams: " + e);
			return false;
		}

		// creates a Thread to listen from the server 
		//multiple threads allow listening multiple servers
		new ThreadListener().start();

		//sends username over for Server to note
		try{
			output.writeObject(username);
		}
		catch (IOException e) {
			System.out.println("Exception during login : " + e);
			close();
			return false;
		}
		return true;
	}
	
	void send(Message msg) {
		try {
			output.writeObject(msg);
		}
		catch(IOException e) {
			System.out.println("Exception writing to server: " + e);
		}
	}

	private void close() {
		try { 
			if(input != null) input.close();
		}
		catch(Exception e) {}
		try {
			if(output != null) output.close();
		}
		catch(Exception e) {}
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {}
			
	}

	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		String serverAddress;
		int portNumber;

		try {
			serverAddress = args[0];
			portNumber = Integer.parseInt(args[1]);
		}

		catch(Exception e) {
			System.out.println("Error in your configurations. Default settings will be used.");
			System.out.println("Server address will be localhost, and Client will be running on port 8561");
			serverAddress = "localhost";
			portNumber = 8561;
		}

		System.out.println("Welcome to the Chat System! Enter your username: ");
		String userName = s.nextLine();

		Client client = new Client(serverAddress, portNumber, userName);

		if(!client.start()){
			s.close();
			return;
		}
		
		System.out.println("Hello.! Welcome to the Chat System.");
		System.out.println("To broadcast a topic, simply type your message into the console.");
		System.out.println("To send a private message, use this format: @username<space>yourmessage");
		System.out.println("To see who is online, type 'ActiveList'");
		System.out.println("To logout, type 'Logout");
		
		// infinite loop to get the input from the user
		while(true) {
			System.out.print("> ");
			String msg = s.nextLine();

			if(msg.equalsIgnoreCase("Logout")) {
				//System.out.println("Logout request received");
				client.send(new Message(2, ""));
				break;
			}

			else if(msg.equalsIgnoreCase("ActiveList")) {
				//System.out.println("ActiveList request received");
				client.send(new Message(0, ""));				
			}

			else{
				//System.out.println("Messaging request received");
				client.send(new Message(1, msg));
			}
		}
		s.close();
		client.close();	
	}

	class ThreadListener extends Thread {

		public void run() {
			//infinite loop to keep listening
			while(true) {
				try {
					String msg = (String) input.readObject();
					System.out.println(msg);
					System.out.print("> ");
				}
				catch(IOException ie) {
					System.out.println("Server has closed the connection: " + ie);
					break;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}

