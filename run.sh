#!/bin/bash

echo "Compiling...!"
javac -d bin/ -cp "derby.jar:src" src/program/Peer.java

echo "Creating peers!"
gnome-terminal -e "java -cp \"derby.jar:bin\" program.Peer 9000;"
for (( c=1; c<3; c++ ))
do  
	gnome-terminal -e "java -cp \"derby.jar:bin\" program.Peer 900$c localhost 9000"
done