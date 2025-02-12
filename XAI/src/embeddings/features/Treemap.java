package embeddings.features;

import llm.java.FlaskServerClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Treemap extends Feature {

    private double[] latentSpace;
    private BufferedImage treemap;

    public Treemap(BufferedImage treemap, FlaskServerClient flaskServerClient) {
        this.treemap = treemap;
        this.latentSpace = extractNumbers(flaskServerClient.sendImage(treemap));
        vectorize();
    }

    public static double[] extractNumbers(String input) {
        // Regular expression to match numbers (digits possibly with decimals or negative sign)
        Pattern pattern = Pattern.compile("-?\\d*\\.?\\d+"); // Matches both integers and floating-point numbers
        Matcher matcher = pattern.matcher(input);

        // List to dynamically store the extracted numbers
        List<Double> numbersList = new ArrayList<>();

        // Extract numbers
        while (matcher.find()) {
            // Parse each matched number and add to the list
            numbersList.add(Double.parseDouble(matcher.group()));
        }

        // Convert list to array
        double[] result = new double[numbersList.size()];
        for (int i = 0; i < numbersList.size(); i++) {
            result[i] = numbersList.get(i);
        }

        return result;
    }

    @Override
    public double distance(Feature other) {
        return 0;
    }

    @Override
    public String print() {
        return "Tree map latent space: " + Arrays.toString(this.latentSpace);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("treeMap", this.latentSpace);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Override
    public double[] vectorize() {
        return new double[0];
    }
}
