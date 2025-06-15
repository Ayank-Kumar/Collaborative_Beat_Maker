import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class Server_Side {
    public static void main(String[] args) {
        clientChannels = new ArrayList<>() ;
        new Server_Side().startUp();
    }

    // non-static can access static.
    static List<ObjectOutputStream> clientChannels ;

    public void startUp(){
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open() ;
            serverSocketChannel.bind(new InetSocketAddress(8080)) ;

            ExecutorService executorService = newCachedThreadPool() ;

            // Server-Socket Channel is a part of non-blocking code.
            while (serverSocketChannel.isOpen()){
                SocketChannel socketChannel = serverSocketChannel.accept() ;
                socketChannel.configureBlocking(true); // Configure socket to be blocking
                //System.out.println("Application Came");

                // Jo message bhejega - use bhi apna message dikhna chahiye ya nahi ?
                // Abhi ke liye to Yes kiya hai.
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketChannel.socket().getOutputStream()) ;
                clientChannels.add(objectOutputStream) ;

                executorService.submit( new eachClient(socketChannel) ) ;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class eachClient implements Runnable {

        SocketChannel socketChannel ;
        public eachClient(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        SerializationClass serializationClass;

        @Override
        public void run() {
            try {
                // Ye Server socketChannel ke saath kaam nahi
                ObjectInputStream objectInputStream = new ObjectInputStream(socketChannel.socket().getInputStream()) ; // thread nai hi banaya

                while( ( serializationClass = (SerializationClass) objectInputStream.readObject() ) != null ){
                        sendToAll(serializationClass);
                }
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void sendToAll(SerializationClass serializationClass ){

        // Yahi issue mujhe bhi lag raha tha
        // Ki Client close to apne side se , server inform nahi
        // To Server Exception handling kr lega
        // Health-check - Jo Respond na kare. Usko remove.
        // Dobara aayega same IP se , tab bhi connection establish krega , fir us wale channel pai communication.
        for (Iterator<ObjectOutputStream> it = clientChannels.iterator(); it.hasNext(); ) {
            ObjectOutputStream objectOutputStream = it.next(); // Iske baad iterator apne aap hi aage barta rehta hai na
            // Iterator man , taki remove kr sake while looping.
            try {
                //System.out.println("Broadcasting: " + serializationClass);
                objectOutputStream.writeObject(serializationClass);
                objectOutputStream.flush();
            } catch (IOException e) {
                System.err.println("Error broadcasting to client: " + e.getMessage());
                it.remove(); // Remove problematic stream
                try {
                    objectOutputStream.close(); // Ensure stream is closed
                } catch (IOException closeEx) {
                    System.err.println("Error closing stream: " + closeEx.getMessage());
                }
            }
        }
    }
}


