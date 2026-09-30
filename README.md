# Typhoon Message notification system

This app is currently utilizing Meta's WhatsApp API.

## How to run the program

### In PowerShell enter:

- $env:WHATSAPP_ACCESS_TOKEN="PASTE_YOUR_TOKEN_HERE"
- $env:WHATSAPP_PHONE_NUMBER_ID="PASTE_YOUR_PHONE_NUMBER_ID_HERE"
- $env:WHATSAPP_API_VERSION="PASTE_META_API_VERSION_HERE"

#### API version must include v (vXX.X)

### set javafx path
- $env:JAVAFX_PATH="C:\path\to\javafx-sdk-21.0.10\lib"  change this path to mathc where ur javafx sdk is

### create output directories
- New-Item -ItemType Directory -Force out
- New-Item -ItemType Directory -Force out\resources

### compile
- javac --module-path "$env:JAVAFX_PATH" --add-modules javafx.controls -d out src\client\Main.java src\client\TCPClient.java src\server\TCPServer.java src\server\WhatsAppService.java

### copy css
- Copy-Item src\resources\javafx.css out\resources\javafx.css -Force

### start server
- java -cp out server.TCPServer

### start JavaFX
- $env:JAVAFX_PATH="C:\path\to\javafx-sdk-21.0.10\lib"


## Run:
- java --module-path "$env:JAVAFX_PATH" --add-modules javafx.controls -cp out client.Main

