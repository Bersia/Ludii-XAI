package embeddings.features;

import embeddings.GroupCode.Cluster;
import embeddings.distance.Jaccard;
import embeddings.distance.utils.HungarianAlgorithm;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import other.context.Context;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Clusters extends Feature {

    private byte BOARD_SIZE;
    private int NUM_COLORS;

    private List<Cluster>[] clusters;
    private List<Byte> colorValues;

    private int numCells = 0;
    private int score = 0;

    public Clusters(Context context, Colors colors) {
//        this.context = currentContext;
        BOARD_SIZE = (byte)Math.sqrt(context.board().graph().faces().size());
        NUM_COLORS = colors.getNumColors();

        clusters = new ArrayList[NUM_COLORS];
        for(int i=0;i<NUM_COLORS;i++){
            clusters[i] = new ArrayList<>();
        }
        colorValues = colors.getColorValues();

        //Initial board
        byte[] board = new byte[BOARD_SIZE*BOARD_SIZE];

        for(int i=0;i<BOARD_SIZE*BOARD_SIZE;i++){
            board[i] = (byte)context.state().containerStates()[0].stateCell(i);
        }

        for(int i=0;i<board.length;i++){
            if(board[i] != 0){
                ArrayList<Integer> clusterShape = new ArrayList<>();
                clusterShape.addAll(findCluster(i, board[i], board));

                int mostWidth = -1;
                int leastWidth = BOARD_SIZE;
                int mostHeight = -1;
                int leastHeight = BOARD_SIZE;
                for(int cell : clusterShape){
                    if (cell%BOARD_SIZE > mostWidth){
                        mostWidth = cell%BOARD_SIZE;
                    }
                    if (cell%BOARD_SIZE < leastWidth){
                        leastWidth = cell%BOARD_SIZE;
                    }
                    if (Math.floor(cell/BOARD_SIZE) > mostHeight){
                        mostHeight = (int) Math.floor(cell/BOARD_SIZE);
                    }
                    if (Math.floor(cell/BOARD_SIZE) < leastHeight){
                        leastHeight = (int) Math.floor(cell/BOARD_SIZE);
                    }
                }
                int width = mostWidth - leastWidth + 1;
                int height = mostHeight - leastHeight + 1;
                byte color = (byte)context.state().containerStates()[0].stateCell(i);
                if(!clusterShape.isEmpty()){
                    clusters[colorValues.indexOf(color)].add(new Cluster(color, clusterShape, height, width));
                    numCells += clusterShape.size();
                    score += (int) Math.pow(clusterShape.size()-1,2);
                }
            }
        }

        for (List<Cluster> clusterList : clusters) {
            clusterList.sort((c1, c2) -> c2.numCells - c1.numCells);
        }
    }

    private ArrayList<Integer> findCluster(int location, byte value, byte[] board) {
        ArrayList<Integer> shape = new ArrayList<>();
        if(board[location] == value){
            shape.add(location);
            board[location] = (byte) 0;

            if ((location - BOARD_SIZE) >= 0) { // Check for tile above
                ArrayList<Integer> subShape = findCluster(location - BOARD_SIZE, value, board);
                if (subShape != null) {
                    shape.addAll(subShape);
                }
            }
            if (((location - 1) % BOARD_SIZE != BOARD_SIZE - 1) && ((location - 1) >= 0)) { // Check for tile on the left
                ArrayList<Integer> subShape = findCluster(location - 1, value, board);
                if (subShape != null) {
                    shape.addAll(subShape);
                }
            }
            if (((location + 1) % BOARD_SIZE != 0) && ((location + 1) < (BOARD_SIZE * BOARD_SIZE))) { // Check for tile on the right
                ArrayList<Integer> subShape = findCluster(location + 1, value, board);
                if (subShape != null) {
                    shape.addAll(subShape);
                }
            }
            if ((location + BOARD_SIZE) < (BOARD_SIZE * BOARD_SIZE)) { // Check for tile below
                ArrayList<Integer> subShape = findCluster(location + BOARD_SIZE, value, board);
                if (subShape != null) {
                    shape.addAll(subShape);
                }
            }

            return shape;
        }
        else {
            return null;
        }
    }

    public int getScore(){
        return score;
    }

    public int getNumCells(){
        return numCells;
    }

    @Override
    public double distance(Feature other) {
        if (!(other instanceof Clusters)) throw new IllegalArgumentException("Features must be of type Clusters");

        Clusters otherClusters = (Clusters) other;

        List<Cluster>[] a = this.clusters.clone();
        int a_size = this.BOARD_SIZE;
        int b_size = otherClusters.BOARD_SIZE;
        List<Cluster>[] b = otherClusters.clusters.clone();


        if(this.BOARD_SIZE != otherClusters.BOARD_SIZE){// changes indexes to make the smallest board a big one mid-game
            if(this.BOARD_SIZE > otherClusters.BOARD_SIZE){// make b the big one
                List<Cluster>[] temp = a;
                a = b;
                b = temp;

                a_size = otherClusters.BOARD_SIZE;
                b_size = this.BOARD_SIZE;
            }
            for(int i=0;i<a.length;i++){
                for(Cluster cluster : a[i]){
                    ArrayList<Integer> newShape = new ArrayList<>();
                    for(int cell : cluster.shape){
                        newShape.add(b_size*cell/a_size+b_size+cell%a_size);
                    }
                    cluster.shape = newShape;
                }
            }
        }

        double[][] costMatrix = new double[a.length][b.length];
        for(int i=0;i<a.length;i++){//for each color combination
            for(int j=0;j<b.length;j++){
                costMatrix[i][j] = getDistanceBetweenClusters(a[i], b[j]);
            }
        }

        HungarianAlgorithm algorithm = new HungarianAlgorithm(costMatrix);
        int [] opt = algorithm.execute();
        double sum=0;
        for (int i = 0; i < opt.length; i++) {
            sum+=costMatrix[i][opt[i]];
        }

        return sum;//TODO: test
    }

    private double getDistanceBetweenClusters(List<Cluster> a, List<Cluster> b) {
        if(a.size()>b.size()){
            List<Cluster> temp = a;
            a = b;
            b = temp;
        }
        double[][] costMatrix = new double[a.size()][b.size()];

        for(int i=0;i<a.size();i++){//for each color combination
            for(int j=0;j<b.size();j++){
                costMatrix[i][j] = Jaccard.distance(a.get(i).shape, b.get(j).shape);
            }
        }

        HungarianAlgorithm algorithm = new HungarianAlgorithm(costMatrix);
        int [] opt = algorithm.execute();

        double sum=0;
        for (int i = 0; i < opt.length; i++) {
            sum+=costMatrix[i][opt[i]];
        }
        return sum;
    }

    @Override
    public String print() {
        StringBuilder print = new StringBuilder("Clusters:");
        for(List<Cluster> clusters : this.clusters){
            for (Cluster cluster : clusters) {
                print.append("\n").append(cluster);
            }
        }

        return print.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray jsonClusters = new JSONArray();
        try {
            for(List<Cluster> clusters:clusters){
                for(Cluster cluster:clusters){
                    jsonClusters.put(cluster.toJSON());
                }
            }
            json.put("clusters", jsonClusters);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Override
    public double[] vectorize() {
        
        return new double[0];//todo: implement
    }

    private class Cluster {
        private final byte color;
        private ArrayList<Integer> shape;
        private final int numCells;
        private final int height;
        private final int width;
        private double[] middleLocation = new double[2];

        public Cluster(byte color, ArrayList<Integer> shape, int height, int width) {
            this.color = color;
            this.shape = shape;
            this.numCells = shape.size();
            this.height = height;
            this.width = width;
            this.middleLocation = findMiddle();
        }

        private double[] findMiddle() {
            double avg_x = 0, avg_y = 0;
            for (int cell : shape) {
                int x = cell % BOARD_SIZE;
                int y = cell / BOARD_SIZE;
                avg_x += x;
                avg_y += y;
            }
            avg_x /= shape.size();
            avg_y /= shape.size();
            return new double[]{avg_x, avg_y};
        }

        @Override
        public String toString() {
            StringBuilder print = new StringBuilder("Color: " + color + ", Size: " + numCells + ", Width: " + width + ", Height: " + height + ", Middle Point: (" + middleLocation[0] + "," + middleLocation[1] + "), Shape: " + shape + "\n");
            for (int j = BOARD_SIZE - 1; j >= 0; j--) {
                for (int i = 0; i < BOARD_SIZE; i++) {
                    String space = "_";
                    for (int cell : shape) {
                        if (cell == i + j * BOARD_SIZE) {
                            space = "X";
                            break;
                        }
                    }
                    print.append(space);
                }
                if (j != 0) print.append("\n");
            }
            return print.toString();
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            try {
                json.put("color", this.color);
                json.put("size", this.numCells);
                json.put("width", this.width);
                json.put("height", this.height);
                json.put("middlePoint", this.middleLocation);
                json.put("shape", this.shape);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return json;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cluster cluster)) return false;
            return color == cluster.color && numCells == cluster.numCells && height == cluster.height && width == cluster.width && Arrays.equals(middleLocation, cluster.middleLocation) && shape.equals(cluster.shape);
        }

        @Override
        public Cluster clone() {
            return new Cluster(this.color, (ArrayList<Integer>) this.shape.clone(), this.height, this.width);
        }
    }
}
