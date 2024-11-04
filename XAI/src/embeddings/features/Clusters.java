package embeddings.features;

import other.context.Context;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Clusters extends Feature {

    private byte BOARD_SIZE;

    private List<Cluster> clusters = new ArrayList<>();
    private int numCells = 0;
    private int score = 0;

    public Clusters(Context context) {
//        this.context = currentContext;
        BOARD_SIZE = (byte)Math.sqrt(context.board().graph().faces().size());

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
                    clusters.add(new Cluster(color, clusterShape, height, width));
                    numCells += clusterShape.size();
                    score += (int) Math.pow(clusterShape.size()-1,2);
                }
            }
        }

        clusters.sort((c1, c2) -> c2.numCells - c1.numCells);
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
        return 0;//TODO:
    }

    @Override
    public String print() {
        StringBuilder print = new StringBuilder("Clusters:");
        for(Cluster cluster : clusters){
            print.append("\n").append(cluster);
        }

        return print.toString();
    }

    private class Cluster {
        private final byte color;
        private final ArrayList<Integer> shape;
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
            avg_x /= shape.size(); avg_y /= shape.size();
            return new double[]{avg_x, avg_y};
        }

        @Override
        public String toString(){
            StringBuilder print = new StringBuilder("Color: " + color + ", Size: " + numCells + ", Width: " + width + ", Height: " + height + ", Middle Point: (" + middleLocation[0] + "," + middleLocation[1] + ")\n");
            for (int j=BOARD_SIZE-1;j>=0;j--){
                for (int i=0;i<BOARD_SIZE;i++){
                    String space = "_";
                    for (int cell : shape){
                        if (cell == i + j*BOARD_SIZE){
                            space = "X";
                            break;
                        }
                    }
                    print.append(space);
                }
                if(j!=0) print.append("\n");
            }
            return print.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cluster cluster)) return false;
            return color == cluster.color && numCells == cluster.numCells && height == cluster.height && width == cluster.width && Arrays.equals(middleLocation, cluster.middleLocation) && shape.equals(cluster.shape);
        }
    }
}
