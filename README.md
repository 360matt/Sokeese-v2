# 💥 Sokeese-v2 - A Socket API


This rich and varied library will greatly facilitate development, it is very light, contains no dependencies, and offers performance almost equal to vanilla sockets

## What's new in v2 ?
- less memory-leaks  
- an event system  
- better management of custom exceptions  
- better multithreading.
- class-based channel system  
- send same object to multiple clients  
- wait for the response of multiple clients on the same request  
- Wait for several type (class) of responses for requests  

## 💙 Features:
* Authentification system (use LoginEvent)
* Retransmission (client -> client(s))
* Reply (A -> server -> B -> server -> A)

## Init a server:
```java
SokeeseServer server = new SokeeseServer(4000);

new Thread(() -> {
  try {
     server.listen(); // blocking method
     // you can do a infinite loop if you want here
     
     System.out.println("server closed"); // example print when the server shutdown
   } catch (IOException e) {
     e.printStackTrace();
  }
}).start();
```

### Clients manager:
```java
ClientsManager manager = server.getClientsManager();

LoggedClient client = manager.get("by name");
LoggedClient client = server.getClient("by name");

manager.disconnect("by name");
server.disconnect("by name");

// _____________________________________________________

Collection<LoggedClient> allClients = manager.getInstances();

manager.disconnectAll();
server.disconnectAll();
```

### Events
```java
        server.addLoginEvent((loginEvent -> {
            loginEvent.getClientName(); // client's name who want to connect
            loginEvent.getPassword(); // client's password who want to connect

            loginEvent.getSocket(); // get client's Socket connection

            // if other registered event has set status to INVALID_CREDENTIALS
            loginEvent.onException(ServerCredentialsException.class, (exEvent) -> {
                // exEvent is ServerCredentialsException type
            });

            loginEvent.onException(IOException.class, (exEvent) -> {
                // catch a IO exception here
            });



            loginEvent.setStatus(StatusCode.OK);
            // the status codes are Integer,
            // you must respect the protocol code by helping you with the StatusCode class.

            loginEvent.setStatus("A custom error message, will be throw by client.");
            // You can define your own status as a String,
            // however, this will be considered an error code.


            // IMPORTANT !!
            // if you define an error code,
            // the event is canceled and the rest of the events will not be executed,
            // so it will be impossible to modify the status code again.


            //you won't need these methods,
            // they will always return 0 - ""
            int code = loginEvent.getStatusCode();
            String custom = loginEvent.getStatusCustom();

            loginEvent.freeze(100);
            // freeze the event for 100ms AFTER all event.

            loginEvent.freeze(0);
            // remove freeze also


        }));
```

### Requests event per type
```java
        server.on(TestObj.class, (obj, request) -> {
            // obj is type of TestObj

            request.getClientName(); // the sender of request

            request.reply(0);
            request.reply("");
            request.reply(new Object());

            LoggedClient loggedClient = request.getClientInstance();
            // get the client instance.
            // you can per example: disconnect, etc ...
        });
```

### ClientLogged instance
```java
LoggedClient client;

client.send(new Object());
client.sendOrThrow(new Object());
// send to a client an object


client.send(new Object(), new Object(), new Object());
client.sendOrThrow(new Object(), new Object(), new Object());
// send to a client multiples objects

client.send(new Object(), (replyBuilder -> {
  replyBuilder.on(String.class, (obj, instance) -> {
    // instance is current instance.
  });
}));
// send to the client and wait reply

client.getClientName();
// get the client name

client.close();
// disconnect / close Socket-in/out

client.getServer();
// get current server.

client.getSocket();
// get Socket connection.
```

### Send data
```java
server.send("recipient's name", new Object());
// send to a client an object

server.send(new String[]{"one", "two"}, new Object());
// send to multiples client an object

server.send("recipient's name", new Object(), new Object());
// send to a client multiples Objects

server.send(new String[]{"one", "two"}, new Object(), new Object());
// send to multiples client multiples Objects

server.send("recipient's name", new Object(), (replyBuilder -> {
  replyBuilder.on(String.class, (obj, client) -> {

  });
 }));
// send to client an object and catch reply


server.send(new String[]{"one", "two", "three"}, new Object(), (replyBuilder -> {
  replyBuilder.on(String.class, (obj, client) -> {

  });
}));
// send to multiple client an object and catch replies
```
