CFLAGS:=-c -Wall -Weffc++ -g 
LDFLAGS:=-lboost_system -lboost_locale -lboost_thread

all: Client

Client: bin/Client.o bin/ConnectionHandler.o 
	g++ bin/Client.o bin/ConnectionHandler.o -o bin/Client -L/usr/lib -lboost_system -lboost_locale -lboost_thread
	
bin/Client.o: src/Client.cpp
	g++ -c -Wall -Weffc++ -g  -lboost_system -lboost_locale -lboost_thread -Linclude src/Client.cpp -o bin/Client.o
	
bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ -c -Wall -Weffc++ -g  -lboost_system -lboost_locale -lboost_thread -Linclude src/ConnectionHandler.cpp -o bin/ConnectionHandler.o
	
clean:
	rm -f bin/*
