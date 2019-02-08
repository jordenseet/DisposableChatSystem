
package aa.ClientServer.Client;
import aa.ClientServer.Client.ThreadListener;
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