#include <stdlib.h>
#include <boost/locale.hpp>
#include "../include/ConnectionHandler.h"
#include <boost/thread.hpp>

	 bool shouldClose=false;

	/**
	 * Handles the input from the user and sends it to the server
	 */
	void msgSend(ConnectionHandler *connectionHandler){
		while(!shouldClose){
			const short bufsize = 1024;
			char buf[bufsize];
			std::cin.getline(buf, bufsize);
			std::string line(buf);
			if (shouldClose)
			  break;
			if (!connectionHandler->sendLine(line)) {
				std::cout << "Disconnected. Exiting...\n" << std::endl;
				break;
			}
		}
	}

	/**
	 * Handles the input from the server and displays it to the user
	 */
	void receive(ConnectionHandler *connectionHandler){
		while(!shouldClose){
			std::string answer;
			if (!connectionHandler->getLine(answer)||answer=="SYSMSG QUIT ACCEPTED \n") {
				shouldClose=true;
			}
			int len=answer.length();
			answer.resize(len-1);
			std:: cout << answer << std::endl;
			if (shouldClose){
			  std::cout << "Disconnecting..." << std::endl;
			  connectionHandler->close();
			}
			  
		}
	}

    int main (int argc, char *argv[]) {
        if (argc < 3) {
            std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
            return -1;
        }
        std::string host = argv[1];
        unsigned short port = atoi(argv[2]);

        ConnectionHandler connectionHandler(host, port);
        if (!connectionHandler.connect()) {
            std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
            return 1;
        }
        boost::thread receiver(receive,&connectionHandler);
        boost::thread sender(msgSend,&connectionHandler);
        
        receiver.join();
	sender.interrupt();
        
        std::cout << "GOOD BYE" << std::endl;

        return 0;
    }


