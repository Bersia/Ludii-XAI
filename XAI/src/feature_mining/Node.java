package feature_mining;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Node {

    public static final int SIZE = 500;

    private int depth;
    private Node parent;
    private List<Node> children = new ArrayList<>();
    private int weight;

    private Node(Node parent, int weight) {
        // root
        if (parent == null) {
            this.depth = 0;
        }
        else {
            this.depth = parent.getDepth() + 1;
            this.weight = weight;
            this.parent = parent;
            this.parent.addChild(this);

        }
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

    public int getWeight() {
        return this.weight;
    }

    public int getDepth() {
        return this.depth;
    }

    public List<Node> getChildren() {
        return this.children;
    }

    private void setParent(Node parent) {
        this.parent = parent;
    }

    private void addWeight(int weight) {
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

    public static List<Rectangle> getTreeMap(Node node, Rectangle box) {
        List<Rectangle> treemap = new ArrayList<>();
        int y = box.getY();
        int x = box.getX();

        for (Node child : node.getChildren()) {
            Rectangle childBox;
            if (child.getDepth() % 2 == 0) {
                int height = (int) Math.round(box.getHeight() * ((double) child.getWeight() / node.getWeight()));
                childBox = new Rectangle(box.getWidth(), height, box.getX(), y);
                y += height;
            }
            else {
                int width = (int) Math.round(box.getWidth() * ((double) child.getWeight() / node.getWeight()));
                childBox = new Rectangle(width, box.getHeight(), x, box.getY());
                x+= width;
            }

            treemap.addAll(getTreeMap(child, childBox));
        }

        treemap.add(box);

        return treemap;
    }

    public static void drawTreeMap(List<Rectangle> treemap) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setBackground(Color.white);
        g.clearRect(0, 0, SIZE, SIZE);
        g.setColor(Color.black);

        for (Rectangle r : treemap) {
            g.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        try {
            ImageIO.write(image, "png", new File("outputs/treemaps/test.png"));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Node a = new Node(null, 0);

        Node b = new Node(a, 1);
        Node c = new Node(a, 0);
        Node d = new Node(a, 0);

        Node e = new Node(c, 1);
        Node f = new Node(c, 0);
        Node g = new Node(c, 1);
        Node h = new Node(d, 1);
        Node i = new Node(d, 1);

        Node j = new Node(f, 1);
        Node k = new Node(f, 1);


        Rectangle box = new Rectangle(SIZE, SIZE, 0, 0);
        List<Rectangle> treemap = Node.getTreeMap(a, box);

        Node.drawTreeMap(treemap);

        System.out.println(treemap);
    }
}
