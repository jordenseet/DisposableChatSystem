package aa.ClientServer.Server;
class ThreadConnector extends Thread {
    Socket socket;
    ObjectInputStream input;
    ObjectOutputStream output;
    int id;
    String username; //just for personalisation purposes
    Message m;

    ThreadConnector(Socket socket) throws Exception {
        this.socket = socket;
        try{
            output = new ObjectOutputStream(socket.getOutputStream());
            input  = new ObjectInputStream(socket.getInputStream());
            // read the username
            username = (String) input.readObject();
        }
        catch (IOException e) {
            System.out.println("Exception creating new Input/output Streams: " + e);
            return;
        }
        catch (Exception e) {
            System.out.println(e);
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
            // get the message from the Message object received
            String message = m.getMessage();

            // different actions based on message type
            
            if (m.getType() == 0){
                //sends currently active clients
                for(int i = 0; i < clients.size(); ++i) {
                    ThreadConnector client = clients.get(i);
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