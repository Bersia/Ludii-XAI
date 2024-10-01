package feature_mining;

import search.mcts.nodes.BaseNode;
import search.mcts.nodes.DeterministicNode;

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
    private int weight = 0;
    private Rectangle graphic;

    public Node(Node parent, int weight) {
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

    public Node(BaseNode node) {
        if (node.parent() == null) {
            this.depth = 0;
        }
        else{
            this.depth = parent.getDepth() + 1;
            //this.weight = 0;
            this.parent = new Node(node.parent());
            //this.parent.addChild(this, false);
        }

        for (BaseNode child : node.getChildren()) {
            this.children.add(new Node(child));
        }

        if (this.children.isEmpty()) {
            this.weight = 1; // TODO retrieve score from mcts
        } else {
            this.children.forEach(child -> this.weight += child.getWeight());
        }
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

    public int getWeight() {
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

    public static void getTreeMap(Node node) {
        Rectangle box; ;
        if (node.getParent() != null) {
            box = node.getGraphic();
        } else {
            box = new Rectangle(SIZE, SIZE, 0, 0);
            node.setGraphic(box);
        }
       // List<Rectangle> treemap = new ArrayList<>();
        int y = box.getY();
        int x = box.getX();

        for (Node child : node.getChildren()) {
            //Rectangle childBox;
            if (child.getDepth() % 2 == 0) {
                int height = (int) Math.round(box.getHeight() * ((double) child.getWeight() / node.getWeight()));
                //childBox = new Rectangle(box.getWidth(), height, box.getX(), y);
                child.setGraphic(new Rectangle(box.getWidth(), height, box.getX(), y));
                y += height;
            }
            else {
                int width = (int) Math.round(box.getWidth() * ((double) child.getWeight() / node.getWeight()));
                //childBox = new Rectangle(width, box.getHeight(), x, box.getY());
                child.setGraphic(new Rectangle(width, box.getHeight(), x, box.getY()));
                x+= width;
            }

            //treemap.addAll(getTreeMap(child, childBox));
            getTreeMap(child);
        }

        //treemap.add(box);

        //return treemap;

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

    public static BufferedImage drawTreeMap(Node node, BufferedImage image, int gradient) {
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(gradient * node.getDepth(), gradient * node.getDepth(), gradient * node.getDepth()));
        Rectangle r = node.getGraphic();
        g.fillRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        g.setColor(Color.black);
        g.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());

        for (Node child : node.getChildren()) {
            //g.setColor(new Color(child.getDepth() * gradient, child.getDepth() * gradient, child.getDepth() * gradient));
            image = drawTreeMap(child, image, gradient);
        }

        return image;
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
        //List<Rectangle> treemap = Node.getTreeMap(a, box);
        Node.getTreeMap(a);

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        int gradient = (int) (255 * (1.0 / a.findMaxDepth()));

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.white);
        graphics.clearRect(0, 0, SIZE, SIZE);

        image = drawTreeMap(a, image, gradient);
        try {
            ImageIO.write(image, "png", new File("outputs/treemaps/test_gradient.png"));
        }catch (IOException exception) {
            exception.printStackTrace();
        }

        //Node.drawTreeMap(treemap);

        //System.out.println(treemap);
    }
}
