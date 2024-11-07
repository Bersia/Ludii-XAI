package feature_mining;

import manager.ai.AIDetails;
import search.mcts.MCTS;
import search.mcts.nodes.BaseNode;
import app.DesktopApp;
import other.AI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Node {

    public static final int SIZE = 1500;

    private int depth;
    private Node parent;
    private List<Node> children;
    private double weight;
    private Rectangle graphic;
    private double score;
    private int sampled;

    public Node() {
        this.depth = 0;
        this.parent = null;
        this.children = new ArrayList<>();
        this.weight = 0.0;
    }

    public static void saveTreemap(DesktopApp app) {
        // Retrieve MCTS agent
        AI ai = app.aiSelected()[1].ai();

        if (! (ai instanceof MCTS)) {
            return;
        }

        MCTS mcts = (MCTS) ai;

        // Find rootnode of the search tree
        BaseNode mctsNode = mcts.rootNode();
        mctsNode = findRoot(mctsNode);

        // Convert search tree to Node type
        Node root = buildTree(mctsNode);

        int maxDepth = root.findMaxDepth();
        double minScore = root.findMinLeafScore();
        double maxScore = root.findMaxLeafScore();
        int maxSampled = root.findMaxSampled();

        // Create and save treemap
        getTreeMap(root, maxDepth, minScore, maxScore, maxSampled);

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.white);
        graphics.clearRect(0, 0, SIZE, SIZE);

        image = drawTreeMap(root, image, "depth");
        try {
            int number = new File("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\depth").list().length;
            ImageIO.write(image, "png", new File(String.format("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\depth\\treemap_%d.png", number)));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);

        graphics = image.createGraphics();
        graphics.setBackground(Color.white);
        graphics.clearRect(0, 0, SIZE, SIZE);

        image = drawTreeMap(root, image, "score");
        try {
            int number = new File("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\score").list().length;
            ImageIO.write(image, "png", new File(String.format("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\score\\treemap_%d.png", number)));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);

        graphics = image.createGraphics();
        graphics.setBackground(Color.white);
        graphics.clearRect(0, 0, SIZE, SIZE);

        image = drawTreeMap(root, image, "sample");
        try {
            int number = new File("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\sample").list().length;
            ImageIO.write(image, "png", new File(String.format("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\sample\\treemap_%d.png", number)));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);

        graphics = image.createGraphics();
        graphics.setBackground(Color.white);
        graphics.clearRect(0, 0, SIZE, SIZE);

        image = drawTreeMap(root, image, "all");
        try {
            int number = new File("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\all").list().length;
            ImageIO.write(image, "png", new File(String.format("C:\\Users\\britt\\IdeaProjects\\Ludii-XAI\\XAI-module\\outputs\\treemaps\\train\\all\\treemap_%d.png", number)));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static Node buildTree(BaseNode node) {
        Node root = new Node();
        root.setScore(node.expectedScore(1) + 2);

        for (BaseNode child : node.getChildren()) {
            Node childNode = buildTree(child);
            childNode.setParent(root);
            root.children.add(childNode);

            if (child.getChildren().isEmpty()) {
                childNode.addWeight(child.expectedScore(1) + 2);
            } else {
                childNode.addWeight(childNode.getWeight());
            }
            childNode.setScore(child.expectedScore(1) + 2);
            childNode.setSampled(child.numVisits());
        }

        return root;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setSampled(int sampled) {
        this.sampled = sampled;
    }

    public Rectangle getGraphic() {
        return this.graphic;
    }

    public int findMaxDepth() {
        int max_depth = this.depth;
        for (Node child : this.children) {
            int current_depth = child.getDepth();

            if (child.getChildren() != null) {
                current_depth = child.findMaxDepth();
            }

            max_depth = Math.max(max_depth, current_depth);
        }

        return max_depth;
    }

    public double findMaxScore() {
        double max_score = this.score;
        for (Node child : this.children) {
            double current_score = child.getScore();

            if (child.getChildren() != null) {
                current_score = child.findMaxScore();
            }

            max_score = Math.max(max_score, current_score);
        }

        return max_score;
    }

    public double findMaxLeafScore() {
        double max_score = - Double.MAX_VALUE;
        for (Node child : this.children) {
            double current_score;
            if (!child.getChildren().isEmpty()) {
                current_score = child.findMaxLeafScore();
            } else {
                current_score = child.getScore();
            }

            max_score = Math.max(max_score, current_score);
        }

        return max_score;
    }

    public double findMinLeafScore() {
        double minScore = Double.MAX_VALUE;
        for (Node child : this.children) {
            double currentScore;
            if (!child.getChildren().isEmpty()) {
                currentScore = child.findMinLeafScore();
            } else {
                currentScore = child.getScore();
            }

            minScore = Math.min(minScore, currentScore);
        }

        return minScore;
    }

    public double findMinScore() {
        double min_score = this.score;
        for (Node child : this.children) {
            double current_score = child.getScore();

            if (child.getChildren() != null) {
                current_score = child.findMinScore();
            }

            min_score = Math.min(min_score, current_score);
        }

        return min_score;
    }

    public int findMaxSampled() {
        int maxSampled = this.sampled;
        for (Node child: this.children) {
            int currentSampled = child.getSampled();

            if (child.getChildren() != null) {
                currentSampled = child.findMaxSampled();
            }

            maxSampled = Math.max(maxSampled, currentSampled);
        }

        return maxSampled;
    }

    public int findMinSampled() {
        int minSampled = this.sampled;
        for (Node child : this.children) {
            int currentSampled = child.getSampled();

            if (child.getChildren() != null) {
                currentSampled = child.findMinSampled();
            }

            minSampled = Math.min(minSampled, currentSampled);
        }

        return minSampled;
    }

    public double getScore() {
        return this.score;
    }

    public int getSampled() {
        return this.sampled;
    }

    public int computeScoreGradient(double minScore, double maxScore) {
        double score = this.score;

        if (score > maxScore) {
            score = maxScore;
        }

        if (minScore < 0) {
            score += Math.abs(minScore);
            maxScore += Math.abs(minScore);
            minScore += Math.abs(minScore);
        }

        double gradient = (score - minScore) / (maxScore - minScore);

        return (int) (255 * gradient);
    }

    public int computeSampleGradient(int maxSampled) {
        return (int) (255 * (1 - (Math.log(this.sampled) / Math.log(maxSampled))));
    }

    public int computeDepthGradient(int maxDepth) {
        return (int) (255 * (this.depth / (double) maxDepth));
    }

    public static BaseNode findRoot(BaseNode node) {
        if (node.parent() == null) {
            return node;
        }

        return findRoot(node.parent());
    }

    public double getWeight() {
        return this.weight;
    }

    public int getDepth() {
        return this.depth;
    }

    public Node getParent() {
        return this.parent;
    }

    public List<Node> getChildren() {
        return this.children;
    }

    public void setGraphic(Rectangle graphic) {
        this.graphic = graphic;
    }

    private void setParent(Node parent) {
        this.parent = parent;
        this.depth = this.parent.getDepth() + 1;

        // propagate depth for all descendants
        for (Node child : this.children) {
            child.setParent(this);
        }
    }

    private void addWeight(double weight) {
        this.weight += weight;

        if (this.parent != null) {
            this.parent.addWeight(weight);
        }
    }

    public void addChild(Node child) {
        this.children.add(child);
        child.setParent(this);
        this.addWeight(child.getWeight());
    }

    public static void getTreeMap(Node node, int maxDepth, double minScore, double maxScore, int maxSampled) {
        Rectangle box;
        if (node.getParent() != null) {
            box = node.getGraphic();
        } else {
            box = new Rectangle(SIZE, SIZE, 0, 0);
            node.setGraphic(box);
        }

        int y = box.getY();
        int x = box.getX();

        for (Node child : node.getChildren()) {
            Rectangle newBox;
            if (child.getDepth() % 2 == 0) {
                int height = (int) Math.round(box.getHeight() * (child.getWeight() / node.getWeight()));
                newBox = new Rectangle(box.getWidth(), height, box.getX(), y);
                y += height;
            }
            else {
                int width = (int) Math.round(box.getWidth() * (child.getWeight() / node.getWeight()));
                newBox = new Rectangle(width, box.getHeight(), x, box.getY());
                x += width;
            }

            newBox.setDepthGradient(child.computeDepthGradient(maxDepth));
            newBox.setScoreGradient(child.computeScoreGradient(minScore, maxScore));
            newBox.setSampleGradient(child.computeSampleGradient(maxSampled));

            child.setGraphic(newBox);

            getTreeMap(child, maxDepth, minScore, maxScore, maxSampled);
        }
    }

    public static BufferedImage drawTreeMap(Node node, BufferedImage image, String type) {
        Graphics2D g = image.createGraphics();
//        g.setColor(new Color(gradient * node.getDepth(), gradient * node.getDepth(), gradient * node.getDepth()));
        Rectangle r = node.getGraphic();

        g.setColor(new Color(255, 255, 255));

        if (type.equals("depth")) {
            g.setColor(new Color(r.getDepthGradient(), r.getDepthGradient(), r.getDepthGradient()));
        } else if (type.equals("score")) {
            g.setColor(new Color(r.getScoreGradient(), r.getScoreGradient(), r.getScoreGradient()));
        } else if (type.equals("sample")) {
            g.setColor(new Color(r.getSampleGradient(), r.getSampleGradient(), r.getSampleGradient()));
        } else {
            g.setColor(new Color(r.getDepthGradient(), r.getScoreGradient(), r.getSampleGradient()));
        }

        g.fillRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        g.setColor(Color.black);
        g.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());

        for (Node child : node.getChildren()) {
            image = drawTreeMap(child, image, type);
        }

        return image;
    }
}
