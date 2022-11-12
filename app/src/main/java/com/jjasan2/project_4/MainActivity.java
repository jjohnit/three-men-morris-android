package com.jjasan2.project_4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Player1Thread player1;
    Player2Thread player2;
    Strategy p1Strategy, p2Strategy;

    // Status of the board. 0 means Empty, 1 means player 1, 2 means player 2
    public int[][] boardStatus = {{0,0,0},{0,0,0},{0,0,0}};
    // Keep track of the moves to stop game at 10 moves
    int movesCounter = 0;

    public static Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                // When player 1 completes a move, update the UI, check for win condition,
                // run the next player
                mHandler.post(new UpdateBoard());
                if(isPlayerWon() || movesCounter > 12){
                    // End game and display message
                    Log.i("appDebug", "Player " + msg.what + " won");
                    player1.interrupt();
                    player2.interrupt();
                    player1 = new Player1Thread();
                    player2 = new Player2Thread();
                    player1.start();
                    player2.start();
                    movesCounter = 0;
                }
                else if (msg.what == 1){
                    player2.p2Handler.post((Runnable) p2Strategy);
                }
                else {
                    player1.p1Handler.post((Runnable) p1Strategy);
                }
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
        startGame();
    }

    // Method to start the game between the players
    protected void startGame(){
//        Handler firstPlayer, secondPlayer;

        // Randomly selects the first player to start the game.\
//        Random randomNumber = new Random();
//        if(randomNumber.nextInt(2) == 0) {
//            firstPlayer = player1.p1Handler;
//            secondPlayer = player2.p2Handler;
//        }
//        else{
//            firstPlayer = player2.p2Handler;
//            secondPlayer = player1.p1Handler;
//        }

//        while (!gameOver && movesCounter <= 12) {
//            firstPlayer.post(new StrategyOffensive(1));
//            secondPlayer.post(new StrategyOffensive(2));
//            movesCounter++;
//        }
//        gameOver = false;

        movesCounter = 0;
        // Randomly selects the strategy to be used by the players
        Random randomGenerator = new Random();
        if (randomGenerator.nextInt(2) == 0){
            p1Strategy = new StrategyOffensive(1);
            Log.i("appDebug", "Player 1 uses strategy - Offensive");
        }
        else {
            p1Strategy = new StrategyRandom(1);
            Log.i("appDebug", "Player 1 uses strategy - Random");
        }
        if (randomGenerator.nextInt(2) == 0){
            p2Strategy = new StrategyOffensive(2);
            Log.i("appDebug", "Player 2 uses strategy - Offensive");
        }
        else {
            p2Strategy = new StrategyRandom(2);
            Log.i("appDebug", "Player 2 uses strategy - Random");
        }
//        p1Strategy = new StrategyRandom(1);
//        p2Strategy = new StrategyRandom(2);
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

            movesCounter++;

            try {
                Thread.sleep(500);
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

            // When there are no pieces in board, add the piece to the first available space.
            if(piecesInBoard == 0){
                boardStatus[possibleMoves.get(0)[0]][possibleMoves.get(0)[1]] = player;
                msg = mHandler.obtainMessage(player);
                mHandler.sendMessage(msg);
                return;
            }
            // When there is 1 piece in the table, add the piece to an adjacent position
            else if(piecesInBoard == 1){
                // Check the adjacent row for free position
                for (int i = 0; i < 3; i++) {
                    if (boardStatus[existingPieces.get(0)[0]][i] == 0){
                        boardStatus[existingPieces.get(0)[0]][i] = player;
                        msg = mHandler.obtainMessage(player);
                        mHandler.sendMessage(msg);
                        return;
                    }
                }
                // Check the adjacent column for free position
                for (int i = 0; i < 3; i++) {
                    if (boardStatus[i][existingPieces.get(0)[1]] == 0){
                        boardStatus[i][existingPieces.get(0)[1]] = player;
                        msg = mHandler.obtainMessage(player);
                        mHandler.sendMessage(msg);
                        return;
                    }
                }
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
                            boardStatus[existingPieces.get(0)[0]][existingPieces.get(0)[1]] = 0;
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

            movesCounter++;

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Random randomGenerator = new Random();
            int row = randomGenerator.nextInt(3);
            int col = randomGenerator.nextInt(3);
            boardStatus[row][col] = player;
            msg = mHandler.obtainMessage(player);
            mHandler.sendMessage(msg);
            return;
        }
    }

    // To update the board in the UI
    public class UpdateBoard implements Runnable {

        @Override
        public void run() {
            // Check for win conditions
            // Update the UI
            Log.i("appDebug", "Move " + movesCounter +" Board : ");
            Log.i("appDebug", boardStatus[0][0] + " " + boardStatus[0][1] + " " + boardStatus[0][2] + " "
                    + boardStatus[1][0] + " " + boardStatus[1][1] + " " + boardStatus[1][2] + " "
                    + boardStatus[2][0] + " " + boardStatus[2][1] + " " + boardStatus[2][2]);
        }
    }
}

