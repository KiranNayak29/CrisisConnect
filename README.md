# CrisisConnect
CrisisConnect

About Crisis Connect:

Crisis Connect is an ad-hoc WIFI network over which all the peers can communicate among each other using chat or audio/video 
calling functionality. This network can be used during disaster recovery as this network won’t be dependent on any cloud based 
server or a service provider.

The peers would use Wi-Fi direct technology to be connected to each other. Once they are connected, one of the peer who initiates 
a call would host itself as a HTTP server and the receiver would act as a client. Once a HTTP connection has been established, they 
can stream audio/video over RTP. When a peer is connected to the server peer, the server peer would receive its IP and discover the 
new client and start communicating with it.


As on 4/7/2017

Status : Project is about 65% complete.

Completed features :
1. Choice to be group owner (start hotspot on single click) 
2. Choice to be a client (Search and connect to a hotspot network on click)
3. Group owner pushes IP of all clients to every client
4. Auto update of list of every available peer for every client.
5. Image sharing achieved.
6. Audio Calling achieved.
7. Connectivity is achieved not only between Group owner and client but also between clients.

Features to be completed.
1. Send Lat lang coordinated along with images.
2. Feature to access these coordinates on Google maps. (we will have a offline saved map of the disaster prone area).
3. Video calling.
4. Texting
5. UI improvements.

New Learning:
1. Issue: Communication was only possible between Group owner and clients. Connectivity between clients was not possible.<br/>
   Solution: We had to somehow let the clients know about the other live clients.<br/>
   How it was achieved: Every linux system has a local ARP table which consists of all the IPs and other info of devices its connected to.
                         We pushed the IPs of every client to all clients, updating their peer list.<br/>
2. Issue: WiFi card availabe on the phone can act either as Group owner or client. Hence a actual live mesh network is not possible <br/>
   Solution : We though of Break and Forward system wherein we can pass on the data by switching between the networks until the data is
             pushed to the owner
             
3. Issues: Variety in Android versions. API level below 23 do not require WRITE_SETTINGS permissions for using WiFi hotspot. Also new API
   24 requires content providers for sharing files.<br/>
   Solution : We have provided support for every problem encountered.
   
   

   
             




