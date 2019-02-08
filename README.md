The Disposable Chat System is a chat system that supports both peer-to-peer and server models. There is no identity management system, no usernames and passwords, and the system does not keep logs of sent messages. This makes it useful for completely anonymous messaging systems, such as declaring confessions online or talking about sensitive problems with strangers.

As this system does not connect via port 80, communication can take place offline (outside the internet). If you are worried about internet-facing threats snooping on your messages, this is a more private environment as well.

First, compile all the java files by running compile.bat

One needs to connect a client to a server in order to communicate.
To start a server, there are two methods:
1) runServer.bat , which will set up a server with the default settings
2) java Server <portnumber> , which will allow you to customise your portnumber. This could be useful for setting up multiple servers in the same host. To demonstrate multithreading, I recommend this option.

To start a client, there are two methods:
1) runClient.bat , which will set up a client with the default settings
2) java Client <networkaddr> <portnumber>  <username>, which will allow you to customise the server you are listening to, the portnumber of the server you are listening to as well as your desired username. To demonstrate multithreading, I recommend this option.
    a) The networkaddr is the IP address of the server you want to communicate with. The default option is 127.0.0.1 or localhost
    b) The portnumber represents the open port from the server. If you try to connect to a closed port, you will get an ConnectException.
    c) The username allows you to customise your userID. The default is "Anonymous"

