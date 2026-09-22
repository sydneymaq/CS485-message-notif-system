# Typhoon Message notification system

## How to run the program

### create a file for compiled .class files: 

Make sure your terminal is at : 
CS485-message-notif-system %

Then use command: 
- mkdir -p out
- javac -d out src/server/TCPServer.java src/client/TCPClient.java

### run
use command: java -cp out server.TCPServer

### create new terminal

create new terminal then run: java -cp out client.TCPClient



