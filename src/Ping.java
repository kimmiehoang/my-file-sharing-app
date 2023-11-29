import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Ping {
    public static void ping(ObjectOutputStream serverOs, ObjectInputStream serverIs, String tempFile, String username)
            throws IOException {
        try {
            ObjectOutputStream outputStream = serverOs;
            ObjectInputStream inputStream = serverIs;

            int packetsSent = 0;
            int packetsReceived = 0;
            String pingContent = "Ping";
            long min = 999999, max = -999999, total = 0;
            // System.out.println("Pinging " +
            // clientSocket.getInetAddress().getHostAddress() + " with "
            // + pingContent.length() + " bytes of data:");

            StringBuilder response = new StringBuilder("\nPing statistics for " + username + "\n");

            while (packetsSent < 4) {
                // Record the time before sending the ping
                long sendTime = System.currentTimeMillis();

                // Send ping to client
                outputStream.writeObject(pingContent);
                outputStream.flush();

                // Receive reply from client
                String clientReply = null;
                clientReply = (String) inputStream.readObject();

                // Record the time after receiving the pong
                long receiveTime = System.currentTimeMillis();

                // Calculate round trip time
                long roundTripTime = receiveTime - sendTime;

                if (clientReply != null) {
                    response.append("\n");
                    response.append("\n- Reply from ").append(username).append(": bytes=").append(clientReply.length())
                            .append(" time=").append(roundTripTime).append("ms content: ").append(clientReply);

                    // System.out.println("Reply from " +
                    // clientSocket.getInetAddress().getHostAddress() +
                    // ": bytes=" + clientReply.length() + " time=" + roundTripTime + "ms content: "
                    // + clientReply);
                    packetsReceived++;
                    if (roundTripTime < min)
                        min = roundTripTime;
                    if (roundTripTime > max)
                        max = roundTripTime;
                    total += roundTripTime;
                } else {
                    response.append("\n");
                    response.append("Reply from ").append(username).append(": No reply");
                    // System.out.println("Reply from " +
                    // clientSocket.getInetAddress().getHostAddress() +
                    // ": No reply");
                }
                packetsSent++;

                // Sleep for 1s before the next ping
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // end ping
            outputStream.writeObject("EndPing");
            outputStream.flush();

            int loss = packetsSent - packetsReceived;
            float percentLost = (packetsSent - packetsReceived) * 100 / packetsSent;
            response.append("\n");
            response.append("\nPackets: Sent = ").append(packetsSent).append(", Received = ").append(packetsReceived)
                    .append(", Lost = ").append(loss).append(" (").append(percentLost).append("% loss)");

            // System.out.println("\nPing statistics for " +
            // clientSocket.getInetAddress().getHostAddress() + ":");
            // System.out.println(" Packets: Sent = " + packetsSent + ", Received = " +
            // packetsReceived +
            // ", Lost = " + (packetsSent - packetsReceived) + " ("
            // + ((packetsSent - packetsReceived) * 100 / packetsSent) + "% loss)");
            response.append("\n");
            response.append("\nApproximate round trip times in milli-seconds:");
            float avg = total / packetsReceived;
            // System.out.println("Approximate round trip times in milli-seconds:");
            response.append("     Minimum = ").append(min).append("ms, Maximum = ").append(max).append("ms, Average = ")
                    .append(avg).append("ms");
            // System.out.println(" Minimum = " + min + "ms, Maximum = " + max +
            // "ms, Average = " + total / packetsReceived + "ms");
            String result = response.toString();
            System.out.println(result);
            FileWriter writer = new FileWriter(tempFile);
            writer.write(result);
            writer.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
