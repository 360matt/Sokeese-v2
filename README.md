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

## Maven
```
<repositories>
    <repository>
        <id>sonatype</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>
<dependency>
    <groupId>io.github.360matt</groupId>
    <artifactId>Sokeese-v2</artifactId>
    <version>2.3-SNAPSHOT</version>
</dependency>
```

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


server.close();
// close the server, close socket, in/out
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

### Login Events
```java
server.getListenerManager().registerListener(new Listener() {
    @Event
    public void onLogin (final LoginEvent event) {
        if (event.getStatusCode().equals(StatusCode.OK)) {

        // do stuff with:
        event.getPassword();
        event.getClientName();

        // and, now, invalide the auth if you want:
        event.setStatusCode(StatusCode.CREDENTIALS);
        event.setStatusCode("custom text");

        }

        System.out.println("try to connect: [id: " + event.getClientName() + "; pwd: " + event.getPassword() + "]");
        // debug only
    }
});
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

## Init a client  
```java
SokeeseClient client_alpha = new SokeeseClient();

new Thread(() -> {
  try {
    client.connect("127.0.0.1", 4000, "name", "password");
    System.out.println("Alpha closed");
  } catch (IOException | RuntimeException | SokeeseException  e) {
    e.printStackTrace();
  }
}).start();

client.close();
// close the client, close socket, in/out


```

## Events
```java
client.on(TestObj.class, (obj, event) -> {
    // obj is type of TestObj

    event.getClientName(); // get sender name

    event.reply(""); // reply String
    event.reply(0); // reply int
    event.reply(new Object()); // reply Object
});
```

## Send data
```java
client.send(new Object());
client.sendOrThrow(new Object());
// send to server an object



client.send("recipient's name", new Object());
client.sendOrThrow("recipient's name", new Object());
// send to a client an object

client.send("recipient's name", new Object(), new Object());
client.sendOrThrow("recipient's name", new Object(), new Object());
// send to a client multiple objects

client.send(new String[]{"one", "two"}, new Object());
client.sendOrThrow(new String[]{"one", "two"}, new Object());
// send to multiple client an object

client.send(new String[]{"one", "two"}, new Object(), new Object());
client.sendOrThrow(new String[]{"one", "two"}, new Object(), new Object());
// send to multiple client multiples objects


// _______________________________________________________________________

client.send("recipient's name", new Object(), replyBuilder -> {
    replyBuilder.on(String.class, (obj, clientName) -> {

    });
});

client.sendOrThrow("recipient's name", new Object(), replyBuilder -> {
    // ...
});

client.sendOrThrow(new String[]{"one", "two"}, new Object(), replyBuilder -> {
    // ...
});

// _______________________________________________________________________


client.send("*", new Object());
// send to everyone without server

client.send("**", new Object());
// send to everyone WITH server

client.send("", new Object());
client.send((String) null, new Object());
// send to server

// reply builder is available for server / everyone
```
