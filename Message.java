package aa.Entities;
import java.io.*;

// We use a message object as it is cleaner to store the type of message in an object as well
public class Message implements Serializable {

	// The different types of message sent by the Client
	// 0 is a call to retrieve all existing users
	// 1 is a normal message
	// 2 is a logout message
	private int type;
	private String message;
	
	public Message(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	public int getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
