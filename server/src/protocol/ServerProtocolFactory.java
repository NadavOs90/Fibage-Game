package protocol;

import tokenizer.StringMessage;

public interface ServerProtocolFactory<T> {
   AsyncServerProtocol<T> create();
}
