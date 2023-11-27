
//import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
import java.util.Scanner;

public class testClient {
    private static String name;

    private static String processInput(String input) {
        // Your logic to process the input and return the result
        if (input.startsWith("hostname")) {
            name = input;
            return "Hello " + input.substring(9); // Assuming "hostname" followed by a name
        } else if (input.startsWith("ping")) {
            return "Live" + name;
        } else {
            return "Unknown command";
        }
    }

    public static void main(String[] args) {
        // Scanner scanner = new Scanner(System.in);
        String tempFile = args[0];

        while (true) {
            try {
                // Đọc lệnh từ tệp tạm
                File file = new File(tempFile);
                Scanner fileScanner = new Scanner(file);

                if (fileScanner.hasNextLine()) {
                    String input = fileScanner.nextLine();
                    fileScanner.close();

                    // Kiểm tra nếu người dùng muốn kết thúc
                    if (input.equals("exit")) {
                        break;
                    }

                    // Process the input
                    String result = processInput(input);
                    // System.out.println(result);
                    // Ghi kết quả vào tệp tạm
                    FileWriter writer = new FileWriter(tempFile);
                    writer.write(result);
                    writer.close();
                }

                // Ngủ để tránh việc đọc liên tục
                Thread.sleep(1000);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Close the scanner to avoid the resource leak
                // scanner.close();
            }
        }
    }

}
