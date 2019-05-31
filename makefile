default:
	javac -d bin/ -cp "derby.jar:src" src/main/PeerMain.java

clean:
	rm -r bin/*
