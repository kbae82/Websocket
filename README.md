Summary
-
Websocket is an Android app to demonstrate Websocket client examples that synchronizes state across other clients

Overall flow
-
Android Websocket protocol -> Python Flask App -> Send the response back to all the connected clients

--------------------------------------------------

Environment
-
Minimum Android Version 5.0 Lolipop SDK 21 up

To run
- You can open the project in Android studio and run 
Or
- Copy and Install APK 'build/outputs/apk/debug/app-debug.apk'

To use
- Run pyWebsocket (server application) and open the js_client.html to see the output
- The console for the server will also print the value out.
- You can use Ngrok or connect the Android Device to the same wifi network with the server.
- Click IP button on Android app and set up the IP
