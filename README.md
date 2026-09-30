# Typhoon Message notification system

This app is currently utilizing Vonage's sandbox API.

## How to run the program

### Set credentials in Powershell:

- $env:VONAGE_API_KEY="PASTE_YOUR_API_KEY"
- $env:VONAGE_API_SECRET="PASTE_YOUR_API_SECRET"
- $env:VONAGE_SANDBOX_NUMBER="PASTE_THE_SANDBOX_WHATSAPP_NUMBER"

### Set sandbox endpoint

- $env:VONAGE_SANDBOX_URL="https://messages-sandbox.nexmo.com/v1/messages"

* make sure it is same as URL in sandbox page

### confirm nonsecret values

- $env:VONAGE_API_KEY
- $env:VONAGE_SANDBOX_NUMBER
- $env:VONAGE_SANDBOX_URL

## Make new powershell terminal

Set path to JavaFX SDK :

- export JAVAFX_PATH=/Users/sydneyquintanilla/Downloads/javafx-sdk-27/lib/

* make sure it matches ur personal javafx lib path

- verify :ls "$JAVAFX_PATH/javafx.controls.jar"

### create output directories

- mkdir -p out/resources

### compile

- javac \ --module-path "$JAVAFX_PATH" \ --add-modules javafx.controls \ -d out \
  src/client/Main.java \
  src/client/TCPClient.java \
  src/server/TCPServer.java \
  src/server/VonageService.java

### copy css

- Copy-Item src\resources\javafx.css out\resources\javafx.css -Force

### Make sure sandbox is active

- go into vonage and scan QR code if needed

### Go back to Terminal with Credentials and start server

- java -cp out server.TCPServer

### Open second terminal and start JavaFX:

- export JAVAFX_PATH=/Users/sydneyquintanilla/Downloads/javafx-sdk-27/lib/

### Run:

- java \
  --module-path "$JAVAFX_PATH" \
  --add-modules javafx.controls \
  -cp out \
  client.Main

## Run procedure after initial compiling is done:

### In terminal 1:

- export VONAGE_API_KEY="a3f45751"
- export VONAGE_API_SECRET="YOUR_REAL_API_SECRET"
- export VONAGE_SANDBOX_NUMBER="14157386102"
- export VONAGE_SANDBOX_URL="https://messages-sandbox.nexmo.com/v1/messages"
- clear
- java -cp out server.TCPServer

### In terminal 2:

(if css was changed first use):

- cp src/resources/javafx.css out/resources/javafx.css

- export JAVAFX_PATH="/Users/sydneyquintanilla/Downloads/javafx-sdk-27/lib/"
- java --module-path "$JAVAFX_PATH" --add-modules javafx.controls -cp out client.Main
