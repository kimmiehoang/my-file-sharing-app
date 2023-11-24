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
                while (!sign) {
                    String responseLine;
                    // Kiểm tra xem có đủ tham số không
                    if (args.length > 0) {
                        // Lấy tham số đầu tiên từ dòng lệnh
                        String command = args[0];

                        // Xử lý lệnh
                        switch (command) {
                            case "HOSTNAME":
                                if (args.length >= 2) {
                                    String hostname = args[1];
                                    os.writeObject(hostname);
                                    os.flush();

                                    os.writeObject(Integer.valueOf(serverSocket.getLocalPort()));
                                    os.flush();
                                    System.out.print("serverSocket: " + serverSocket.getInetAddress());

                                    os.writeObject(serverSocket.getInetAddress());
                                    os.flush();

                                    responseLine = (String) is.readObject();
                                    System.out.println(responseLine);
                                    System.out.println(
                                            "Follow these syntaxes:\n1. PUBLISH filename filepath\n2. FETCH filename\n3. quit: log out");

                                } else {
                                    System.out.println("Invalid HOSTNAME command. Usage: HOSTNAME name");
                                }
                                break;

                            case "PUBLISH":
                                // Xử lý lệnh PUBLISH
                                if (args.length >= 3) {
                                    String filename = args[1];
                                    String filepath = args[2];
                                    os.writeObject("PUBLISH");
                                    os.flush();

                                    publish(filename, filepath);

                                    responseLine = (String) is.readObject();
                                    System.out.println(responseLine);
                                } else {
                                    System.out.println("Invalid PUBLISH command. Usage: PUBLISH filename filepath");
                                }
                                break;

                            case "FETCH":
                                if (args.length >= 2) {
                                    String filename = args[1];
                                    os.writeObject("FETCH");
                                    os.flush();

                                    responseLine = (String) is.readObject();
                                    if (responseLine.equals("")) {
                                        System.out.println("Sorry. No clients have got your requested file");
                                        // continue;
                                    }
                                    System.out.println(responseLine);

                                    System.out.print("Select one hostname from hostname list above: ");
                                    String targetClient = inputLine.readLine().trim();

                                    os.writeObject(targetClient);
                                    os.flush();

                                    int portNum = ((Integer) is.readObject()).intValue();

                                    InetAddress targetAddr = (InetAddress) is.readObject();
                                    System.out.println(targetAddr);
                                    Socket peerClient = new Socket(targetAddr, portNum);

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
                                    System.out.println("Invalid FETCH command. Usage: FETCH filename");
                                }
                                break;
                            case "QUIT":
                                responseLine = (String) is.readObject();
                                System.out.println(responseLine);
                                if (responseLine.startsWith("Goodbye")) {
                                    sign = true;
                                }

                            default:
                                System.out.println("Unknown command: " + command);
                                break;
                        }
                    } else {
                        System.out.println("No command provided.");
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
        String folderPath = "receivedFile/";
        String filePath = folderPath + filename;

        // Kiểm tra xem thư mục lưu trữ đã tồn tại chưa
        File folder = new File(folderPath);
        if (!folder.exists()) {
            // Nếu chưa tồn tại, tạo mới
            if (folder.mkdirs()) {
                System.out.println("Folder created: " + folderPath);
            }
        }

        // Kiểm tra xem file đã tồn tại chưa
        File file = new File(filePath);
        int i = 1;
        while (file.exists()) {
            filePath = filePath + "(" + Integer.toString(i) + ")";
            file = new File(filePath);
            i++;
            // return;
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
    public void run() {
        try {
            while (true) {
                Socket peerServer = serverSocket.accept();
                System.out.println("peer server work");
                peerConnection peerHandler = new peerConnection(peerServer);
                peerHandler.start();
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
