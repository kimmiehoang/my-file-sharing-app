import java.io.*;
import java.net.*;

public class Client implements Runnable {

    private static Socket socketOfClient = null;
    private static ServerSocket serverSocket = null;
    private static ObjectOutputStream os = null;
    private static ObjectInputStream is = null;
    private static BufferedReader inputLine = null;
    private static boolean sign = false;
    private static FileRepository localRepository;

    public static void main(String[] args) {
        try {

            socketOfClient = new Socket("localhost", 9000);
            serverSocket = new ServerSocket(0);
            new Thread(new Client()).start();
            os = new ObjectOutputStream(socketOfClient.getOutputStream());
            is = new ObjectInputStream(socketOfClient.getInputStream());
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            localRepository = new FileRepository();
            System.out.println("listening server on port: " + socketOfClient.getLocalPort()
                    + ", listening peer connection on port: " + serverSocket.getLocalPort());

        } catch (UnknownHostException e) {
            System.err.println("UnknownHost");
        } catch (IOException e) {
            System.err.println("No Server found");
        }

        if (socketOfClient != null && serverSocket != null && os != null && is != null) {
            try {

                System.out.print("Enter your hostname: ");
                String hostname = inputLine.readLine().trim();

                os.writeObject(hostname);
                os.flush();

                os.writeObject(Integer.valueOf(serverSocket.getLocalPort()));
                os.flush();
                System.out.print("serverSocket: " + serverSocket.getInetAddress());

                os.writeObject(serverSocket.getInetAddress());
                os.flush();

                String responseLine = (String) is.readObject();
                System.out.println(responseLine);
                System.out.println(
                        "Follow these syntaxes:\n1. PUBLISH filename filepath\n2. FETCH filename\n3. quit: log out");

                // Thread peerHandlerThread = new Thread(new Client());
                // peerHandlerThread.start();
                // new Thread(new Client()).start();
                while (!sign) {

                    System.out.print("Enter your command: ");
                    String cmd = inputLine.readLine().trim();

                    if (cmd.startsWith("PUBLISH")) {
                        os.writeObject(cmd);
                        os.flush();
                        String[] data = cmd.split(" ");
                        publish(data[1], data[2]);

                        responseLine = (String) is.readObject();
                        System.out.println(responseLine);

                    } else if (cmd.startsWith("FETCH")) {
                        os.writeObject(cmd);
                        os.flush();

                        String[] data = cmd.split(" ");
                        String filename = data[1];

                        responseLine = (String) is.readObject();
                        if (responseLine.equals("")) {
                            System.out.println("Sorry. No clients have got your requested file");
                            continue;
                        }
                        System.out.println(responseLine);

                        System.out.print("Select one hostname from hostname list above: ");
                        String targetClient = inputLine.readLine().trim();

                        // System.out.println("this client is " + targetClient);

                        os.writeObject(targetClient);
                        os.flush();

                        int portNum = ((Integer) is.readObject()).intValue();
                        // System.out.println(portNum);

                        InetAddress targetAddr = (InetAddress) is.readObject();
                        System.out.println(targetAddr);
                        Socket peerClient = new Socket(targetAddr, portNum);

                        /*
                         * byte[] fileContent = "tien".getBytes("UTF-8");
                         * String newFileName = "newFile.txt";
                         * 
                         * saveFile(newFileName, fileContent);
                         */
                        ObjectOutputStream peerOs = new ObjectOutputStream(peerClient.getOutputStream());
                        ObjectInputStream peerIs = new ObjectInputStream(peerClient.getInputStream());

                        peerOs.writeObject(filename);
                        peerOs.flush();

                        /////// xử lý file content nhận được

                        byte[] fileContent = (byte[]) peerIs.readObject();
                        System.out.println(fileContent);

                        String newFileName = "newFile.txt";
                        saveFile(newFileName, fileContent);

                        localRepository.files.put(filename, fileContent);

                        System.out.println(filename + " was downloaded successfully");

                        peerIs.close();
                        peerOs.close();
                        peerClient.close();
                    } else {
                        responseLine = (String) is.readObject();
                        System.out.println(responseLine);
                        if (responseLine.startsWith("Goodbye")) {
                            sign = true;
                        }
                    }
                }

                os.close();
                is.close();
                socketOfClient.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    //////////////////////////////////

    private static void publish(String fileName, String filePath) {
        localRepository.publish(fileName, filePath);
    }

    private static void saveFile(String filename, byte[] fileContent) {
        // Lưu nội dung file vào thư mục
        // Trong ví dụ này, mình giả sử thư mục lưu trữ file nằm trong thư mục gốc của
        // dự án
        String folderPath = "receivedFile/";
        String filePath = folderPath + filename;

        // Kiểm tra xem file đã tồn tại chưa
        File file = new File(filePath);
        if (file.exists()) {
            System.out.println("File already exists: " + filename);
            return;
        }

        // Tạo mới file nếu nó chưa tồn tại
        try {
            if (file.createNewFile()) {
                System.out.println("File created: " + filename);
            } else {
                System.err.println("Failed to create file: " + filename);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Ghi nội dung vào file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(fileContent);
            System.out.println("File saved: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() { // hàm này ko chạy
        try {
            while (true) {
                // System.out.println("condition work");
                Socket peerServer = serverSocket.accept();
                System.out.println("peer server work");
                // ObjectInputStream is = new ObjectInputStream(peerServer.getInputStream());
                // String requestedFile = (String) is.readObject();
                // System.out.println(requestedFile);
                peerConnection peerHandler = new peerConnection(peerServer);
                peerHandler.start();
                // System.out.println("problem here ");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class peerConnection extends Thread {
        private Socket peerServer;
        private ObjectInputStream is;
        private ObjectOutputStream os;

        public peerConnection(Socket peerServer) {
            this.peerServer = peerServer;
        }

        public void run() {
            try {
                is = new ObjectInputStream(peerServer.getInputStream());
                System.out.println(is);
                os = new ObjectOutputStream(peerServer.getOutputStream());
                // chỗ này ko work
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String requestedFile = (String) is.readObject();
                System.out.println(requestedFile);

                System.out.println("The requested file is " + requestedFile);
                byte[] fileContent = localRepository.getFileContent(requestedFile);

                os.writeObject(fileContent);
                os.flush();

                is.close();
                is.close();
                peerServer.close();

            } catch (IOException | ClassNotFoundException e) {
                System.err.print("A FileTransfer process went wrong");
            }
        }
    }

}
