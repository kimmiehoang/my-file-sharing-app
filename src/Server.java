import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Server implements Runnable {

    private class ClientInfo {
        public String hostname;
        public int portNum;
        public InetAddress addr;
        public Socket socketOfServer;
        public ArrayList<String> listOfFiles;

        public ClientInfo(String hostname, int portNum, InetAddress addr, Socket socketOfServer) {
            this.hostname = hostname;
            this.portNum = portNum;
            this.addr = addr;
            this.socketOfServer = socketOfServer;
            listOfFiles = new ArrayList<String>();
        }
    }

    private static ServerSocket serverSocket;
    private static ArrayList<ClientInfo> listOfClients = new ArrayList<ClientInfo>();
    private static boolean sign = false;
    private static String tempFileContent;

    public void handleClient(Socket socketOfServer) {
        try {
            System.out.println("Server work ok");
            ObjectInputStream is = new ObjectInputStream(socketOfServer.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(socketOfServer.getOutputStream());

            String hostname = (String) is.readObject();

            int portOfClientListener = ((Integer) is.readObject()).intValue();

            InetAddress addr = (InetAddress) is.readObject();
            
            listOfClients.add(new ClientInfo(hostname, portOfClientListener, addr, socketOfServer));

            os.writeObject(
                    "Your information has been recorded. From now on, you will operate under the name: " + hostname);
            os.flush();

            while (true) {
                String cmd = (String) is.readObject();
                if (cmd.startsWith("PUBLISH")) {
                    
                    String[] data = cmd.split(" ");
                    String filename = data[1];
                    for (ClientInfo cur : listOfClients) {
                        if (cur.hostname.equals(hostname) && cur.socketOfServer == socketOfServer) {
                            cur.listOfFiles.add(filename);
                            break;
                        }
                    }
                    os.writeObject(filename
                            + " was published successfully. This information has been recorded by the server.");
                    os.flush();

                } else if (cmd.startsWith("FETCH")) {
                    System.out.println(hostname + " is conducting a fetch cmd");
                    String[] data = cmd.split(" ");
                    String filename = data[1];

                    String list = SearchClients(filename);
                    os.writeObject(list);
                    os.flush();
                    if (list.equals("")) {
                        continue;
                    }
                    else {
                        for (ClientInfo cur : listOfClients) {
                            if (cur.hostname.equals(hostname) && cur.socketOfServer == socketOfServer) {
                                cur.listOfFiles.add(filename);
                                break;
                            }
                        }
                    }
                    

                } else if (cmd.startsWith("CHOOSE")) {
                    String targetClient = (String) is.readObject();
                    int portNum = getPort(targetClient);
                    InetAddress targetAddr = getAddr(targetClient);
                    
                    os.writeObject(Integer.valueOf(portNum));
                    os.flush();

                    os.writeObject(targetAddr);
                    os.flush();

                } else if (cmd.startsWith("QUIT")) {
                    
                    break;
                } else {
                    os.writeObject("Your command is invalid.");
                    os.flush();
                }
            }

            is.close();
            os.close();
            socketOfServer.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Class Not Found.");
        }

    }

    public String SearchClients(String requestedFile) {
        // Search for clients that have the requested file
        StringBuilder response = new StringBuilder(
                "*********************************\nClients with the requested file:");
        boolean sign = false;
        for (ClientInfo cur : listOfClients) {
            if (cur.listOfFiles.indexOf(requestedFile) != -1) {
                response.append("\n- ").append(cur.hostname);
                sign = true;
            }
        }
        response.append("\n*********************************");

        if (!sign)
            return "";

        return response.toString();
    }

    public static int getPort(String hostname) {
        int portNum = 0;
        for (ClientInfo client : listOfClients) {
            if (client.hostname.equals(hostname)) {
                portNum = client.portNum;
            }
        }
        return portNum;
    }

    public static InetAddress getAddr(String hostname) {
        InetAddress addr = null;
        for (ClientInfo client : listOfClients) {
            if (client.hostname.equals(hostname)) {
                addr = client.addr;
            }
        }
        return addr;
    }


    @Override
    public void run() {
        try {
            while (true) {
                Socket socketOfServer = serverSocket.accept();
                Thread clientHandler = new Thread(() -> handleClient(socketOfServer));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {

            System.out.println("Starting Server....");
            serverSocket = new ServerSocket(9000);
            System.out.println("Server Socket has been created. Server is running on port 9000");

        } catch (IOException e) {
            System.out.println("Server Socket can't be created");
        }
        new Thread(new Server()).start();

        String tempFile = args[0];

        while (!sign) {
            try {
                File file = new File(tempFile);
                Scanner fileScanner = new Scanner(file);

                if (fileScanner.hasNextLine()) {
                    tempFileContent = fileScanner.nextLine();
                    fileScanner.close();

                    if (tempFileContent.startsWith("DISCOVER")) {
                        String[] info = tempFileContent.split(" ");
                        String hostname = info[1];

                        StringBuilder response = new StringBuilder(
                                "**************************************************\nList of files in local repository of host named "
                                        + hostname);                                    
                        for (ClientInfo cur : listOfClients) {
                            if (cur.hostname.equals(hostname)) {
                                for (String nameOfFile : cur.listOfFiles) {
                                    response.append("\n- ").append(nameOfFile);
                                }
                                break;
                            }
                        }
                        response.append("\n**********************************************************");
                        

                        String result = response.toString();
                        System.out.println(result);
                        FileWriter writer = new FileWriter(tempFile);
                        writer.write(result);
                        writer.close();

                    } else if (tempFileContent.startsWith("PING")) {
                        String[] info = tempFileContent.split(" ");
                        String hostname = info[1];

                        for (ClientInfo cur : listOfClients) {
                            if (cur.hostname.equals(hostname)) {
                                int portNum = getPort(cur.hostname);
                                InetAddress targetAddr = getAddr(cur.hostname);
                                Socket serverToClient = new Socket(targetAddr, portNum);

                                ObjectOutputStream serverOs = new ObjectOutputStream(serverToClient.getOutputStream());
                                ObjectInputStream serverIs = new ObjectInputStream(serverToClient.getInputStream());
                                Ping.ping(serverOs, serverIs, tempFile, cur.hostname);
                                serverIs.close();
                                serverOs.close();
                                serverToClient.close();
                                break;
                            }
                        }
                        
                    } else if (tempFileContent.startsWith(" QUIT")) {
                        sign = true;
                    }

                }
                Thread.sleep(1000);
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }

    }
}