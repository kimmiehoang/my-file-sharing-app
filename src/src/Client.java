import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Scanner;

public class Client implements Runnable {

    private static Socket socketOfClient = null;
    private static ServerSocket serverSocket = null;
    private static ObjectOutputStream os = null;
    private static ObjectInputStream is = null;
    private static boolean sign = false;
    private static FileRepository localRepository;
    private static String host;
    private static String tempFileContent;
    private static Socket peerClient = null;
    private static ObjectOutputStream peerOs = null;
    private static ObjectInputStream peerIs = null;

    public static void main(String[] args) {
        try {
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                        .getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    if (networkInterface.getName().startsWith("w")) {
                        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                        while (inetAddresses.hasMoreElements()) {
                            InetAddress inetAddress = inetAddresses.nextElement();
                            if (!inetAddress.isLoopbackAddress()
                                    && inetAddress.getHostAddress().indexOf(":") == -1) {
                                host = inetAddress.getHostAddress();
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
            serverSocket = new ServerSocket(0, 50, InetAddress.getByName(host));
            new Thread(new Client()).start();

        } catch (IOException e) {
            System.out.println("Server Socket can't be created");
        }

        String tempFile = args[0];
        while (!sign) {
            try {
                File file = new File(tempFile);
                Scanner fileScanner = new Scanner(file);

                if (fileScanner.hasNextLine()) {
                    tempFileContent = fileScanner.nextLine();
                    fileScanner.close();

                    ///////////////////////
                    if (tempFileContent.equals("")) {
                        try {
                            socketOfClient = new Socket(InetAddress.getLocalHost(), 9000);

                            os = new ObjectOutputStream(socketOfClient.getOutputStream());
                            is = new ObjectInputStream(socketOfClient.getInputStream());
                            localRepository = new FileRepository();
                        } catch (UnknownHostException e) {
                            try {
                                String result = "UnknownHost";
                                System.out.println(result);
                                FileWriter writer = new FileWriter(tempFile);
                                writer.write(result);
                                writer.close();
                                continue;

                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }
                        } catch (IOException e) {
                            try {
                                String result = "No Server found";
                                System.out.println(result);
                                FileWriter writer = new FileWriter(tempFile);
                                writer.write(result);
                                writer.close();
                                continue;

                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (socketOfClient != null && serverSocket != null && os != null && is != null) {

                            try {
                                String result = "Start successfully";
                                System.out.println(result);
                                FileWriter writer = new FileWriter(tempFile);
                                writer.write(result);
                                writer.close();
                                Thread.sleep(1000);
                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }

                        }

                    } else if (tempFileContent.startsWith("HOSTNAME") || tempFileContent.startsWith("PUBLISH")
                            || tempFileContent.startsWith("FETCH") || tempFileContent.startsWith("CHOOSE")
                            || tempFileContent.startsWith("QUIT")) {

                        ////////////////////////
                        try {
                            String cmd = tempFileContent;
                            if (cmd.startsWith("HOSTNAME")) {

                                String[] data = cmd.split(" ");

                                os.writeObject(data[1]);
                                os.flush();

                                os.writeObject(Integer.valueOf(serverSocket.getLocalPort()));
                                os.flush();

                                os.writeObject(serverSocket.getInetAddress());
                                os.flush();

                                String res = (String) is.readObject();

                                try {
                                    String result = "Welcome " + data[1] + "! " + res;
                                    System.out.println(result);
                                    FileWriter writer = new FileWriter(tempFile);
                                    writer.write(result);
                                    writer.close();

                                } catch (Exception ee) {
                                    ee.printStackTrace();
                                }

                            } else if (cmd.startsWith("PUBLISH")) {
                                os.writeObject(cmd);
                                os.flush();
                                String[] data = cmd.split(" ");
                                publish(data[1], data[2]);
                                String res = (String) is.readObject();

                                try {
                                    String result = res;
                                    System.out.println(result);
                                    FileWriter writer = new FileWriter(tempFile);
                                    writer.write(result);
                                    writer.close();

                                } catch (Exception ee) {
                                    ee.printStackTrace();
                                }

                            } else if (cmd.startsWith("FETCH")) {
                                os.writeObject(cmd);
                                os.flush();

                                String responseLine = (String) is.readObject();
                                if (responseLine.equals("")) {

                                    try {
                                        String result = "Sorry. No clients have got your requested file";
                                        System.out.println(result);
                                        FileWriter writer = new FileWriter(tempFile);
                                        writer.write(result);
                                        writer.close();

                                    } catch (Exception ee) {
                                        ee.printStackTrace();
                                    }
                                } else {
                                    try {
                                        String result = responseLine
                                                + " Select one hostname from hostname list above! Using CHOOSE hostname filename";
                                        System.out.println(result);
                                        FileWriter writer = new FileWriter(tempFile);
                                        writer.write(result);
                                        writer.close();

                                    } catch (Exception ee) {
                                        ee.printStackTrace();
                                    }
                                }

                            } else if (cmd.startsWith("CHOOSE")) {
                                os.writeObject(cmd);
                                os.flush();

                                String[] data = cmd.split(" ");
                                String targetClient = data[1];
                                String filename = data[2];

                                os.writeObject(targetClient);
                                os.flush();

                                int portNum = ((Integer) is.readObject()).intValue();

                                InetAddress targetAddr = (InetAddress) is.readObject();

                                peerClient = new Socket(targetAddr, portNum);

                                peerOs = new ObjectOutputStream(peerClient.getOutputStream());
                                peerIs = new ObjectInputStream(peerClient.getInputStream());

                                peerOs.writeObject(filename);
                                peerOs.flush();


                                byte[] fileContent = (byte[]) peerIs.readObject();

                                String newFileName = "newFile.txt";
                                saveFile(newFileName, fileContent);

                                try {
                                    String result = filename + " was downloaded successfully";
                                    System.out.println(result);
                                    FileWriter writer = new FileWriter(tempFile);
                                    writer.write(result);
                                    writer.close();

                                } catch (Exception ee) {
                                    ee.printStackTrace();
                                }

                                localRepository.files.put(filename, fileContent);

                                peerIs.close();
                                peerOs.close();
                                peerClient.close();
                            } else if (cmd.startsWith("QUIT")) {
                                os.writeObject("QUIT");
                                os.close();
                                is.close();
                                socketOfClient.close();
                                sign = true;

                            }

                            Thread.sleep(1000);

                        } catch (IOException | ClassNotFoundException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Thread.sleep(1000);

            } catch (Exception ee) {
                ee.printStackTrace();
            }

        }

    }

    //////////////////////////////////

    private static void publish(String fileName, String filePath) {
        localRepository.publish(fileName, filePath);
    }

    private static void saveFile(String filename, byte[] fileContent) {
        String folderPath = "../receivedFile/";
        String originalFilePath = folderPath + filename;
        String filePath = originalFilePath;

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
            filePath = originalFilePath + "(" + Integer.toString(i) + ")";
            file = new File(filePath);
            i++;

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
            while (!sign) {
                Socket peerServer = serverSocket.accept();
                peerConnection peerHandler = new peerConnection(peerServer);
                peerHandler.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class peerConnection extends Thread {
        private Socket peerServer;
        private ObjectInputStream peerIs;
        private ObjectOutputStream peerOs;

        public peerConnection(Socket peerServer) {
            this.peerServer = peerServer;
        }

        public void run() {
            try {
                peerIs = new ObjectInputStream(peerServer.getInputStream());
                peerOs = new ObjectOutputStream(peerServer.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String request = (String) peerIs.readObject();
                if (request.startsWith("Ping")) {
                    if (sign == true) {
                        peerIs.close();
                        peerOs.close();
                        peerServer.close();
                    }

                    while (!request.startsWith("EndPing")) {
                        peerOs.writeObject(
                                "Received ping message successfully at " + System.currentTimeMillis());
                        peerOs.flush();
                        request = (String) peerIs.readObject();
                    }
                    peerIs.close();
                    peerOs.close();
                    peerServer.close();
                } else {
                    if (sign == true) {
                        peerIs.close();
                        peerOs.close();
                        peerServer.close();
                    }
                    String requestedFile = request;

                    byte[] fileContent = localRepository.getFileContent(requestedFile);

                    peerOs.writeObject(fileContent);
                    peerOs.flush();

                    peerIs.close();
                    peerOs.close();
                    peerServer.close();
                }

            } catch (IOException | ClassNotFoundException e) {
                System.err.print("A FileTransfer process went wrong");
            }
        }
    }

}