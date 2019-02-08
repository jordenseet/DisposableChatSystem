package aa.ClientServer;
import aa.Entities.Message;
import java.security.NoSuchAlgorithmException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;


public class Client  {

	// requires object input/output streams to send Message object over to servers/peers
	private ObjectInputStream input;
	private ObjectOutputStream output;
	// require sockets to connect to servers/peers
	private Socket socket;
	
	// configurations to connect to a desired server

	// networkaddr is ip address, such as 192.168.1.1 or localhost
	private String networkaddr;
	// port refers to the port of the ip address to communicate with
	private int port;

	// Username of this client
	private String username;

	public String getUsername() {
		return username;
	}
	
	Client(String networkaddr, int port, String username) {
		this.networkaddr = networkaddr;
		this.port = port;
		this.username = username;
	}
	
	public boolean start() {
		try {
			//creates socket to communicate with desired server/peer
			socket = new Socket(networkaddr, port);
		} 
		catch(Exception e) {
			System.out.println("Error connecting to server:" + e);
			return false;
		}
		
		System.out.println("Connection accepted by " + socket.getInetAddress() + ":" + socket.getPort());

		try{

			//create streams to communicate messages with server/peers
			input  = new ObjectInputStream(socket.getInputStream());
			output = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException e) {
			System.out.println("Exception creating new Input/output Streams: " + e);
			return false;
		}

		// creates a Thread to listen from the server 
		// multiple threads allow listening multiple servers
		new ListeningThread().start();

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
			// sends a Message object to the object output stream
			output.writeObject(msg);
		}
		catch(IOException e) {
			System.out.println("Exception writing to server: " + e);
		}
	}

	//closes everything to clean up
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

	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		String serverAddress;
		int portNumber;

		try {
			//reads arguments from command line
			serverAddress = args[0];
			portNumber = Integer.parseInt(args[1]);
		}

		catch(Exception e) {
			System.out.println("Detected running from bat file, or you did not specify which server and port to connect to.\nDefault configurations will be used.");
			System.out.println("Server address will be localhost, and Client will be running on port 8561");
			System.out.println("To connect to a specific server, please use 'java Client <networkaddr> <portnumber>'");
			serverAddress = "localhost";
			portNumber = 8561;
		}

		System.out.println("Welcome to the Chat System! Enter your username: ");
		String userName = s.nextLine();

		int rng = new Random().nextInt(Integer.MAX_VALUE); //random number generator to create unique ids
		long timestamp = new Date().getTime(); //timestamp to make it even more unique

		String toHash = userName + rng + timestamp; // unique ID without hashing
		String uniqueId = toHash; // in case hashing fails, this is backup unique id

		try{
			//creates a message digest using the SHA256 hashing algorith,
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			//converts to array of bytes as hashing uses bitwise operators
			byte[] hashed = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
			String minihash = Base64.getEncoder().encodeToString(hashed).substring(0,7); //6 letters to improve usability
			uniqueId = userName + "/" +  minihash;			
		}
		catch(NoSuchAlgorithmException ea){
			System.out.println("Error trying to hash uniqueID, please settle for this ugly looking uniqueID");
		}

		System.out.println("Your unique identifier is " + uniqueId);

		// Create new Client object with all parameters finally defined
		Client client = new Client(serverAddress, portNumber, uniqueId);

		if(!client.start()){
			s.close();
			return;
		}
		try{
			System.out.println("Hello.! Welcome to the Chat System.");
			System.out.println("To broadcast a topic, simply type your message into the console.");
			System.out.println("To send a private message, use this format: @username<space>yourmessage");
			System.out.println("To see who is online, type 'ActiveList'");
			System.out.println("To logout, type 'Logout'");
			
			// infinite loop to get the input from the user
			while(true) {
				System.out.print("> ");
				String msg = s.nextLine();

				if(msg.equalsIgnoreCase("Logout")) {
					// if receive request to logout
					client.send(new Message(2, ""));
					break;
				}

				else if(msg.equalsIgnoreCase("ActiveList")) {
					// if receive request to see currently active users
					client.send(new Message(0, ""));				
				}

				else{
					// sends message to intended audience
					client.send(new Message(1, msg));
				}
			}	
		}
		catch(Exception e){
			//in case of exception when using platform, logs out user
			client.send(new Message(2, ""));
		}
		finally{
			//ensure we clean up
			s.close();
			client.close();				
		}
	}

	class ListeningThread extends Thread {

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

