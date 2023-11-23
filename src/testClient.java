import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class testClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("0.0.0.0", 51195);
            byte[] fileContent = "tien".getBytes("UTF-8");

            // Send the byte array to the server
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(fileContent, 0, fileContent.length);
            outputStream.flush();

            // Close the socket
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
