compile with:

make
-or-
javac -d bin/ -cp "derby.jar:src" src/main/PeerMain.java




launch first peer with:

java -cp "derby.jar:bin" main.PeerMain <server_port>

launch each following peers with;

java -cp "derby.jar:bin" main.PeerMain <server_port> <ip_peer1> <port_peer1>
