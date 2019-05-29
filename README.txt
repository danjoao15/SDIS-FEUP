Para compilar o programa:

make
*ou*
javac -d bin/ -cp "derby.jar:src" src/program/Peer.java 

Para correr o primeiro peer:

java -cp "derby.jar:bin" program.Peer <server_port>

Para correr os peers seguintes:

java -cp "derby.jar:bin" program.Peer <server_port> <ip_peer1> <port_peer1>

