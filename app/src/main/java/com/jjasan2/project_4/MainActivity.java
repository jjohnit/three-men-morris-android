package com.jjasan2.project_4;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Player1Thread player1;
    Player2Thread player2;
    boolean gameOver = false;

    // Status of the board. 0 means Empty, 1 means player 1, 2 means player 2
    int[][] boardStatus = {{0,0,0},{0,0,0},{0,0,0}};
    // Keep track of the moves to stop game at 10 moves
    int movesCounter = 0;

    Handler mHandler;

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
            firstPlayer.post(new StrategyOffensive());
            secondPlayer.post(new StrategyDefensive());
            movesCounter++;
        }
        gameOver = false;
        movesCounter = 0;
    }
}

