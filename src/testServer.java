import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class testServer implements Runnable {

    private static ServerSocket serverSocket = null;
    private boolean sign = false;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(3000);
            new Thread(new testServer()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (!sign) {
                System.out.println("here");
                Socket peerServer = serverSocket.accept();
                System.out.println("peer server is running");
                // Triển khai logic xử lý kết nối ở đây
                // new Thread(new PeerHandler(peerServer)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
