import app.DesktopApp;
import llm.java.FlaskServerClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Main {
    private static DesktopApp desktopApp = null;
//    private static FlaskServerClient flaskServerClient; // Instance of the client

    public static void main(final String[] args) {
        // Initialize the Flask server client, which starts the server
//        flaskServerClient = new FlaskServerClient();

        // Initialize the Java desktop application
        desktopApp = new DesktopApp();
        desktopApp.createDesktopApp();

//        // Interact with the Flask server
//        String jsonResponse = flaskServerClient.sendPrompt("Hello, how are you?", 3, 250);
//        System.out.println("Response from Flask server: " + jsonResponse);

        // Optionally upload files
//        File[] filesToUpload = { new File("path/to/file1.pdf"), new File("path/to/file2.pdf") };
//        String uploadResponse = flaskServerClient.uploadFiles(filesToUpload);
//        System.out.println("Response from file upload: " + uploadResponse);

        // Stop the Flask server when done
//        flaskServerClient.stopFlaskServer();
    }
}
