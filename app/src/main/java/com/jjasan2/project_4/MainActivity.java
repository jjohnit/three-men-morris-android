package com.jjasan2.project_4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Player1Thread player1;
    Player2Thread player2;
    Strategy p1Strategy, p2Strategy;
    final int START_PLAYER_1 = 1;
    int playerWon = 0;

    ImageView piece00, piece01, piece02, piece10, piece11, piece12,
            piece20, piece21, piece22;
    ImageView[][] imageBoard;

    // Status of the board. 0 means Empty, 1 means player 1, 2 means player 2
    public int[][] boardStatus;
    // Keep track of the moves to stop game at 10 moves
    int movesCounter = 0;

    public static Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        piece00 = findViewById(R.id.piece_00);
        piece01 = findViewById(R.id.piece_01);
        piece02 = findViewById(R.id.piece_02);
        piece10 = findViewById(R.id.piece_10);
        piece11 = findViewById(R.id.piece_11);
        piece12 = findViewById(R.id.piece_12);
        piece20 = findViewById(R.id.piece_20);
        piece21 = findViewById(R.id.piece_21);
        piece22 = findViewById(R.id.piece_22);

        imageBoard = new ImageView[][] {{piece00, piece01, piece02}, {piece10, piece11, piece12},
                {piece20, piece21, piece22}};

        mHandler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                // When player 1 completes a move, update the UI, check for win condition,
                // run the next player
                movesCounter++;
                mHandler.post(new UpdateBoard());
                // Stops the game when one player wins the game or there is tie
                // Tie if the no one wins after 12 moves.
                if(!isPlayerWon() && movesCounter <= 12){
                    if (msg.what == START_PLAYER_1){
                        player2.p2Handler.post((Runnable) p2Strategy);
                    }
                    else {
                        player1.p1Handler.post((Runnable) p1Strategy);
                    }
                }
                else
                    playerWon = msg.what;
                return;
            }
        };
        player1 = new Player1Thread();
        player2 = new Player2Thread();
        player1.start();
        player2.start();
    }

    // Method called on clicking the 'Start Game' button in UI
    public void onClickStartGame(View view) {
        // Stop the threads and restart if they are already running
        if(player1.isAlive() || player2.isAlive()){
            player1.interrupt();
            player2.interrupt();
            player1 = new Player1Thread();
            player2 = new Player2Thread();
            player1.start();
            player2.start();
        }

        // Wait for the Handlers to start
        while (true){
            if(player1.p1Handler != null && player2.p2Handler != null)
                break;
        }
        boardStatus = new int[][] {{0,0,0},{0,0,0},{0,0,0}};
        startGame();
    }

    // Method to start the game between the players
    protected void startGame(){

        movesCounter = 0;
        // Randomly selects the strategy to be used by the players
        TextView p1StrategyText = findViewById(R.id.p1_strategy_text);
        TextView p2StrategyText = findViewById(R.id.p2_strategy_text);
        Random randomGenerator = new Random();
        if (randomGenerator.nextInt(2) == 0){
            p1Strategy = new StrategyOffensive(1);
            p1StrategyText.setText("Player 1 uses strategy - Offensive");
            Log.i("appDebug", "Player 1 uses strategy - Offensive");
            p2Strategy = new StrategyRandom(2);
            p2StrategyText.setText("Player 2 uses strategy - Random");
            Log.i("appDebug", "Player 2 uses strategy - Random");
        }
        else {
            p1Strategy = new StrategyRandom(1);
            p1StrategyText.setText("Player 1 uses strategy - Random");
            Log.i("appDebug", "Player 1 uses strategy - Random");
            p2Strategy = new StrategyOffensive(2);
            p1StrategyText.setText("Player 1 uses strategy - Offensive");
            Log.i("appDebug", "Player 2 uses strategy - Offensive");
        }
        findViewById(R.id.strategy_desc_table).setVisibility(View.VISIBLE);
        // Starts the game by calling the first player
        player1.p1Handler.post((Runnable) p1Strategy);
    }

    // To check whether any player won
    public boolean isPlayerWon(){
        // Check whether any complete row have same value
        for (int i = 0; i < 3; i++) {
            if((boardStatus[i][0] > 0) && (boardStatus[i][0] == boardStatus[i][1])
                    && (boardStatus[i][1] == boardStatus[i][2])){
                return true;
            }
        }
        // Check whether any complete column have same value
        for (int i = 0; i < 3; i++) {
            if((boardStatus[0][i] > 0) && (boardStatus[0][i] == boardStatus[1][i])
                    && (boardStatus[1][i] == boardStatus[2][i])){
                return true;
            }
        }
        return false;
    }

    // Base class for strategies
    public class Strategy {

        int player;
        Message msg = mHandler.obtainMessage(player);

        public Strategy(int player) {
            this.player = player;
            msg.what = player;
        }
    }

    // Strategy focussed on victory
    public class StrategyOffensive extends Strategy implements Runnable {

        public StrategyOffensive(int player) {
            super(player);
        }

        @Override
        public void run() {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int piecesInBoard = 0;
            List<Integer[]> possibleMoves = new ArrayList<Integer[]>();
            List<Integer[]> existingPieces = new ArrayList<Integer[]>();

            // Check whether pieces are available or need to replace existing in the board.
            for (int i = 0; i < boardStatus.length; i++) {
                for (int j = 0; j < boardStatus[i].length; j++) {
                    if(boardStatus[i][j] == player){
                        piecesInBoard++;
                        existingPieces.add(new Integer[]{i, j});
                    }
                    else if (boardStatus[i][j] == 0)
                        possibleMoves.add(new Integer[]{i,j});
                }
            }

            // Only one thread should modify the board at a time.
            synchronized (boardStatus){
                // When there are no pieces in board, add the piece to the first available space.
                if(piecesInBoard == 0){
                    boardStatus[possibleMoves.get(0)[0]][possibleMoves.get(0)[1]] = player;
                    msg = mHandler.obtainMessage(player);
                    mHandler.sendMessage(msg);
                    return;
                }
                // When there is 1 piece in the table, add the piece to an adjacent position
                else if(piecesInBoard == 1){
                    boolean boardUpdatedFlag = false;
                    // Check the adjacent row for free position
//                    for (int i = 0; i < 3; i++) {
//                        if (boardStatus[existingPieces.get(0)[0]][i] == 0){
//                            boardStatus[existingPieces.get(0)[0]][i] = player;
//                            msg = mHandler.obtainMessage(player);
//                            mHandler.sendMessage(msg);
//                            return;
//                        }
//                    }

                    switch (existingPieces.get(0)[0]){
                        case 0:
                        case 2:
                            if (boardStatus[existingPieces.get(0)[0]][1] == 0){
                                boardStatus[existingPieces.get(0)[0]][1] = player;
                                boardUpdatedFlag = true;
                            }
                            break;
                        case 1:
                            if (boardStatus[existingPieces.get(0)[0]][0] == 0){
                                boardStatus[existingPieces.get(0)[0]][0] = player;
                                boardUpdatedFlag = true;
                            }
                            else if (boardStatus[existingPieces.get(0)[0]][2] == 0){
                                boardStatus[existingPieces.get(0)[0]][2] = player;
                                boardUpdatedFlag = true;
                            }
                            break;
                    }

                    if (boardUpdatedFlag){
                        msg = mHandler.obtainMessage(player);
                        mHandler.sendMessage(msg);
                        return;
                    }

                    // Check the adjacent column for free position
                    switch (existingPieces.get(0)[1]) {
                        case 0:
                        case 2:
                            if (boardStatus[1][existingPieces.get(0)[1]] == 0) {
                                boardStatus[1][existingPieces.get(0)[1]] = player;
                                boardUpdatedFlag = true;
                            }
                            break;
                        case 1:
                            if (boardStatus[0][existingPieces.get(0)[1]] == 0) {
                                boardStatus[0][existingPieces.get(0)[1]] = player;
                                boardUpdatedFlag = true;
                            }
                            else if (boardStatus[2][existingPieces.get(0)[1]] == 0) {
                                boardStatus[2][existingPieces.get(0)[1]] = player;
                                boardUpdatedFlag = true;
                            }
                            break;
                    }
//                    for (int i = 0; i < 3; i++) {
//                        if (boardStatus[i][existingPieces.get(0)[1]] == 0){
//                            boardStatus[i][existingPieces.get(0)[1]] = player;
//                            msg = mHandler.obtainMessage(player);
//                            mHandler.sendMessage(msg);
//                            return;
//                        }
//                    }
                    if (boardUpdatedFlag){
                        msg = mHandler.obtainMessage(player);
                        mHandler.sendMessage(msg);
                        return;
                    }

                    // If unable to find an adjacent position, use any free position
                    boardStatus[possibleMoves.get(0)[0]][possibleMoves.get(0)[1]] = player;
                    msg = mHandler.obtainMessage(player);
                    mHandler.sendMessage(msg);
                    return;
                }
                else if(piecesInBoard == 2){
                    // Check for possible win conditions
                    boolean pieceAdded = checkWinConditions(existingPieces.get(0), existingPieces.get(1));
                    // If the existing pieces cannot be utilized for win condition,
                    // add the piece in an available position
                    if (!pieceAdded)
                        boardStatus[possibleMoves.get(0)[0]][possibleMoves.get(0)[1]] = player;
                    msg = mHandler.obtainMessage(player);
                    mHandler.sendMessage(msg);
                    return;
                }
                // When there's 3 pieces in the board, replace one
                else{
                    for (int i = 0; i < 2; i++) {
                        for (int j = i + 1; j < 3; j++) {
                            boolean pieceAdded = checkWinConditions(existingPieces.get(i), existingPieces.get(j));
                            // if win condition found, remove the extra piece from board
                            if (pieceAdded){
                                List<Integer[]> positionToRemove = new ArrayList<>(existingPieces);
                                positionToRemove.remove(existingPieces.get(i));
                                positionToRemove.remove(existingPieces.get(j));
                                boardStatus[positionToRemove.get(0)[0]][positionToRemove.get(0)[1]] = 0;
                                msg = mHandler.obtainMessage(player);
                                mHandler.sendMessage(msg);
                                return;
                            }
                        }
                    }
                    // If the existing pieces cannot be utilized for win condition,
                    // add the piece in an available position and remove an existing piece
                    boardStatus[possibleMoves.get(0)[0]][possibleMoves.get(0)[1]] = player;
                    boardStatus[existingPieces.get(0)[0]][existingPieces.get(0)[1]] = 0;
                    msg = mHandler.obtainMessage(player);
                    mHandler.sendMessage(msg);
                    return;
                }
            }
        }

        public boolean checkWinConditions(Integer[] piece1, Integer[] piece2){
            // If the existing pieces are in same row.
            if (piece1[0] == piece2[0]){
                for (int i = 0; i < 3; i++) {
                    if (boardStatus[piece1[0]][i] == 0){
                        boardStatus[piece1[0]][i] = player;
                        return true;
                    }
                }
            }
            // If the existing pieces are in same column.
            else if (piece1[1] == piece2[1]){
                for (int i = 0; i < 3; i++) {
                    if (boardStatus[i][piece1[1]] == 0){
                        boardStatus[i][piece1[1]] = player;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // Strategy focussed on defending/preventing the other player from winning
    public class StrategyRandom extends Strategy implements Runnable {

        public StrategyRandom(int player) {
            super(player);
        }

        @Override
        public void run() {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Random randomGenerator = new Random();

            // Only one thread should modify the board at a time.
            synchronized (boardStatus){
                while (true){
                    int row = randomGenerator.nextInt(3);
                    int col = randomGenerator.nextInt(3);
                    if (boardStatus[row][col] == 0){
                        boardStatus[row][col] = player;
                        msg = mHandler.obtainMessage(player);
                        mHandler.sendMessage(msg);
                        return;
                    }
                }
            }
        }
    }

    // To update the board in the UI
    public class UpdateBoard implements Runnable {

        @Override
        public void run() {
            // Update the UI
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (boardStatus[i][j] == 0){
                        imageBoard[i][j].setVisibility(View.INVISIBLE);
                    }
                    else if (boardStatus[i][j] == 1){
                        imageBoard[i][j].setImageResource(R.drawable.plus_icon);
                        imageBoard[i][j].setVisibility(View.VISIBLE);
                    }
                    else if (boardStatus[i][j] == 2){
                        imageBoard[i][j].setImageResource(R.drawable.cross_icon);
                        imageBoard[i][j].setVisibility(View.VISIBLE);
                    }
                }
            }
            // Check for win conditions
            if (isPlayerWon()){
                Log.i("appDebug", "Player " + playerWon +" won");
            }
        }
    }
}

