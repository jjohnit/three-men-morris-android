package com.jjasan2.project_4;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Player1Thread player1;
    Player2Thread player2;
    // To sequentially run the threads based on the player.
    int nextPlayer = 1;
    boolean gameOver = false;

    // Status of the board. 0 means Empty, 1 means player 1, 2 means player 2
    public int[][] boardStatus = {{0,0,0},{0,0,0},{0,0,0}};
    // Keep track of the moves to stop game at 10 moves
    int movesCounter = 0;

    public static Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(getMainLooper());
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
        Handler firstPlayer, secondPlayer;
        Random randomNumber = new Random();

        // Randomly selects the first player to start the game.
        if(randomNumber.nextInt(2) == 0) {
            firstPlayer = player1.p1Handler;
            secondPlayer = player2.p2Handler;
        }
        else{
            firstPlayer = player2.p2Handler;
            secondPlayer = player1.p1Handler;
        }

        while (!gameOver && movesCounter <= 12) {
            nextPlayer = 1;
            firstPlayer.post(new StrategyOffensive(1));
            nextPlayer = 2;
            secondPlayer.post(new StrategyOffensive(2));
            movesCounter++;
        }
        gameOver = false;
        movesCounter = 0;
    }

    // Strategy focussed on victory
    public class StrategyOffensive implements Runnable {

        int player;

        public StrategyOffensive(int player) {
            this.player = player;
        }

        @Override
        public void run() {
            Log.i("appDebug", "Offensive strategy by player " + player);

            while (player != nextPlayer){
                // Wait for the players chance
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
            }
            // When there is 1 piece in the table, add the piece to an adjacent position
            else if(piecesInBoard == 1){
                // Check the adjacent row for free position
                for (int i = 0; i < 3; i++) {
                    if (boardStatus[existingPieces.get(0)[0]][i] == 0){
                        boardStatus[existingPieces.get(0)[0]][i] = player;
                        mHandler.post(new UpdateBoard(player));
                        return;
                    }
                }
                // Check the adjacent column for free position
                for (int i = 0; i < 3; i++) {
                    if (boardStatus[i][existingPieces.get(0)[1]] == 0){
                        boardStatus[i][existingPieces.get(0)[1]] = player;
                        mHandler.post(new UpdateBoard(player));
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
                mHandler.post(new UpdateBoard(player));
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
                            mHandler.post(new UpdateBoard(player));
                            return;
                        }
                    }
                }
                // If the existing pieces cannot be utilized for win condition,
                // add the piece in an available position and remove an existing piece
                boardStatus[possibleMoves.get(0)[0]][possibleMoves.get(0)[1]] = player;
                boardStatus[existingPieces.get(0)[0]][existingPieces.get(0)[1]] = 0;
                mHandler.post(new UpdateBoard(player));
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
    public class StrategyDefensive implements Runnable {

        int player;

        public StrategyDefensive(int player) {
            this.player = player;
        }

        @Override
        public void run() {
            Log.i("appDebug", "Defensive strategy by player " + player);
        }
    }

    // To update the board in the UI
    public class UpdateBoard implements Runnable {
        int player;
        public UpdateBoard(int player) {
            this.player = player;
        }

        @Override
        public void run() {
            // Check for win conditions
            // Update the UI
            Log.i("appDebug", "Player " + player + " updated board to ");
            Log.i("appDebug", boardStatus[0][0] + " " + boardStatus[0][1] + " " + boardStatus[0][2] + " "
                    + boardStatus[1][0] + " " + boardStatus[1][1] + " " + boardStatus[1][2] + " "
                    + boardStatus[2][0] + " " + boardStatus[2][1] + " " + boardStatus[2][2]);
        }
    }
}

