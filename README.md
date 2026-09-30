# Typhoon Message notification system

This app is currently utilizing Vonage's sandbox API.

## How to run the program

### Set credentials in Powershell:
- $env:VONAGE_API_KEY="PASTE_YOUR_API_KEY"
- $env:VONAGE_API_SECRET="PASTE_YOUR_API_SECRET"
- $env:VONAGE_SANDBOX_NUMBER="PASTE_THE_SANDBOX_WHATSAPP_NUMBER"

### Set sandbox endpoint
- $env:VONAGE_SANDBOX_URL="https://messages-sandbox.nexmo.com/v1/messages" * make sure it is same as URL in sandbox page

### confirm nonsecret values
- $env:VONAGE_API_KEY
- $env:VONAGE_SANDBOX_NUMBER
- $env:VONAGE_SANDBOX_URL

## Make new powershell terminal
Set path to JavaFX SDK : 
- $env:JAVAFX_PATH="C:\path\to\javafx-sdk-21.0.10\lib" * make sure it matches ur javafx sdk location

### create output directories
- New-Item -ItemType Directory -Force out
- New-Item -ItemType Directory -Force out\resources

### compile
- javac --module-path "$env:JAVAFX_PATH" --add-modules javafx.controls -d out src\client\Main.java src\client\TCPClient.java src\server\TCPServer.java src\server\VonageService.java

### copy css
- Copy-Item src\resources\javafx.css out\resources\javafx.css -Force

### Go back to Terminal with Credentials and start server
- java -cp out server.TCPServer

### Open second terminal and start JavaFX:
- $env:JAVAFX_PATH="C:\path\to\javafx-sdk-21.0.10\lib"


## Run:
- java --module-path "$env:JAVAFX_PATH" --add-modules javafx.controls -cp out client.Main

