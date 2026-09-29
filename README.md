# Adaptive-Video-Streaming
A client-server Java application for adaptive video streaming over TCP/UDP based on real time bandwidth testing, using FFmpeg for media transcoding and streaming.

## Requires
Java Development Kit 
Apache Maven
FFmpeg and FFplay 

## Project Structure
Client.java: The client-side application with a Swing GUI, bandwidth tester, and playback controller.
Server.java: The multithreaded server responsible for file management, client requests, and streaming via FFmpeg.
pom.xml: Maven configuration file.


