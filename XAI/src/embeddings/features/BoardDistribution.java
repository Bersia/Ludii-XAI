package embeddings.features;

import embeddings.distance.DTW;
import other.context.Context;

import java.util.List;

public class BoardDistribution extends Feature {

    private static final byte EMPTY = 0;
    private static final byte THIS = 1;
    private static final byte OTHER = 2;

    private byte[][][] board;

    private byte BOARD_SIZE;
    private int COLOURS;

    public BoardDistribution(Context context, Colors colors) {
        BOARD_SIZE = (byte)Math.sqrt(context.board().graph().faces().size());
        COLOURS = colors.getNumColors();
        List<Byte> colorValues = colors.getColorValues();

        //Initial board
        board = new byte[COLOURS+1][BOARD_SIZE][BOARD_SIZE];
        for(int j=BOARD_SIZE-1;j>=0;j--){
            for(int k=0;k<BOARD_SIZE;k++) {
                byte piece = (byte)context.state().containerStates()[0].stateCell(BOARD_SIZE * j + k);
                board[0][BOARD_SIZE-1-j][k] = piece;
                for(int c=1;c<=COLOURS;c++){
                    board[c][BOARD_SIZE-1-j][k] = piece==colorValues.get(c-1)?THIS:(piece==0?EMPTY:OTHER);
                }
            }
        }
    }

    public BoardDistribution(byte[][] boardOriginal) {
//        this.measurer = new DTW();



        BOARD_SIZE = (byte) boardOriginal.length;
//        COLOURS = Math.max(board);

        //Initial board
        board = new byte[COLOURS+1][BOARD_SIZE][BOARD_SIZE];
        for(int j=0;j<BOARD_SIZE;j++){
            for(int k=0;k<BOARD_SIZE;k++) {
                byte piece = boardOriginal[j][k];
                board[0][j][k] = piece;
                for(int c=1;c<=COLOURS;c++){
                    board[c][j][k] = piece==c?THIS:(piece==0?EMPTY:OTHER);
                }
            }
        }
    }

    @Override
    public double distance(Feature other) {
        return DTW.distance(board, ((BoardDistribution)other).board);
    }

    @Override
    public String print() {
        StringBuilder print = new StringBuilder("Board Distribution:\n");
        for(int i=0;i<BOARD_SIZE;i++) {//rows
            for(int j=0;j<board.length;j++) {//colors
                for(int k=0;k<BOARD_SIZE;k++) {//columns
                    print.append(board[j][i][k]).append(" ");
                }
                print.append("    ");
            }
            print.append("\n");
        }
        return print.toString();
    }
}
