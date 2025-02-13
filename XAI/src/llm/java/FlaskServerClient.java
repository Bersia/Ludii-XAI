package llm.java;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.Base64;

public class FlaskServerClient {

    private static final String BASE_URL = "http://127.0.0.1:5000"; // Base URL for the Flask server
    private Process flaskProcess; // To store the Flask server process

    // Load the .env file
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("XAI")  // Set the correct path to your .env file
            .load();

    // Retrieve the ACCESS_TOKEN
    private static final String ABSOLUTE_PATH = dotenv.get("ABSOLUTE_PATH");

    // Constructor
    public FlaskServerClient() {
        startFlaskServer(); // Start the Flask server when the client is instantiated
        // Add shutdown hook to stop the Flask server when the application is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopFlaskServer));
    }

    // Method to start the Flask server
    private void startFlaskServer() {
        try {
            // Command to start Flask server, update the script path as needed
            String pythonInterpreter = ABSOLUTE_PATH+"..\\venv\\Scripts\\python.exe"; // Path to your Python interpreter
            String scriptPath = ABSOLUTE_PATH+"src\\llm\\python\\RagServer\\src\\app.py"; // Path to your script

            // Construct the command
            ProcessBuilder builder = new ProcessBuilder(pythonInterpreter, scriptPath);

            // Set the working directory (optional, if required by the script)
            builder.directory(new File(ABSOLUTE_PATH+"src\\llm\\python\\RagServer"));

            // Redirect error and output streams (optional, for debugging)
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            // Start the Flask server process
            flaskProcess = builder.start();

            // Wait for the server to be healthy
            if (!waitForServerHealth()) {
                System.out.println("Failed to start Flask server. Exiting.");
            } else {
                System.out.println("Flask server started successfully.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to wait for the Flask server to become healthy
    private boolean waitForServerHealth() {
        String healthCheckUrl = BASE_URL + "/health"; // URL of your health check endpoint
        int retries = 20; // Number of retries
        int sleepTime = 5000; // Time to wait between retries (in milliseconds)

        for (int i = 0; i < retries; i++) {
            try {
                URL url = new URL(healthCheckUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000); // Timeout for connecting
                conn.setReadTimeout(2000); // Timeout for reading response

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    System.out.println("Server is healthy and running.");
                    return true;
                } else {
                    System.out.println("Server is not healthy. Response Code: " + responseCode);
                }
            } catch (Exception e) {
                System.out.println("Failed to connect to Flask server. Retrying...");
            }

            // Wait before retrying
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        return false; // Server did not become healthy in the given retries
    }

    // Method to send a prompt to the Flask server
    public String sendPrompt(String prompt, int k, int maxNewTokens, JSONObject features) {
        try {
            // Define the URL for the /generate endpoint
            URL url = new URL(BASE_URL + "/generate");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            // Create the JSON payload
            JSONObject payload = new JSONObject();
            payload.put("prompt", prompt);
            payload.put("k", k);
            payload.put("max_new_tokens", maxNewTokens);
            payload.put("features", features); // Add the features object


            String jsonInputString = payload.toString();

            System.out.println(jsonInputString);
            JSONObject response = new JSONObject();
            response.put("response", jsonInputString);
            return response.toString();

            // Send the JSON input to the server
//            try (OutputStream os = con.getOutputStream()) {
//                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
//                os.write(input, 0, input.length);
//            }
//
//            // Read the response
//            StringBuilder response = new StringBuilder();
//            try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
//                String responseLine;
//                while ((responseLine = br.readLine()) != null) {
//                    response.append(responseLine.trim());
//                }
//            }
//            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String sendImage(BufferedImage image) {
        try {
            // Convert BufferedImage to Base64 String
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Define the URL for the /treemap endpoint
            URL url = new URL(BASE_URL + "/treemap");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            // Create JSON payload
            String jsonInputString = "{\"img\": \"" + base64Image + "\"}";

            // Send JSON input to the server
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    // Method to stop the Flask server
    public void stopFlaskServer() {
        if (flaskProcess != null) {
            flaskProcess.destroy();
            System.out.println("Flask server stopped.");
        }
    }
}
