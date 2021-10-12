package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }

    private static final int TIMEOUT_MILISECONDS = 5000;

    private long start;
    private boolean timeout;
    private static final int INITIAL_DEPTH = 5;
    private int currentDepth;

    private int bestMove;
    private int globalBestMove;

    public int which_player(GameState board) {
        if (board.getNextPlayer() == 2) {
            return 1;
        } else {
            return 2;
        }
    }

    // B-version
    public int calculateBestMove(GameState currentBoard)
    {
        currentDepth = 0;

        timeout = false;
        start = System.currentTimeMillis();

        // depth iterated approach.
        for(int depth = 0;; depth++) {

            // We only want completed bestmoves.
            if (depth > 0) {
                globalBestMove = bestMove;
                System.out.println("Completed search with depth" + currentDepth + ". Best move so far: " + globalBestMove);
            }

            currentDepth = INITIAL_DEPTH + depth;

            int currentPlayer = currentBoard.getNextPlayer();

            // int currentPlayer = which_player(currentBoard);

            minimax_B(currentBoard, currentDepth, currentPlayer, Integer.MIN_VALUE, Integer.MAX_VALUE);

            if (timeout) {
                System.out.println("Search done!");
                return globalBestMove;
            }
        }
    }

    /**
     *
     *
     * @param copyboard
     * @param depth
     * @param isMaximizing
     * Either a 1 or a 2, representing the current players turn. Max is 1, Min is 2.
     * @return
     */

    public int minimax_B(GameState copyboard, int depth, int isMaximizing, int alpha, int beta) {
        boolean isTerminal = copyboard.gameEnded();

        //int secondPlayer = 1;
        //if (isMaximizing == 1) {
        //    secondPlayer = 2;
        //}

        int secondPlayer = 1;
        if (player == 1) {
            secondPlayer = 2;
        }


        int score_result = copyboard.getScore(player) - copyboard.getScore(secondPlayer);
        // int score_result = copyboard.getScore(isMaximizing) - copyboard.getScore(secondPlayer);

        // could probably be under the same if statement
        if (System.currentTimeMillis() - start > TIMEOUT_MILISECONDS) {
            timeout = true;

            // If the game is over or we reach depth 0, then we return the score of current player minus the score
            // of the other player
            return score_result;
        }

        if (isTerminal || depth == 0) {
            // If the game is over or we reach depth 0, then we return the score of current player minus the score
            // of the other player

            return score_result;
        }

        if (isMaximizing == 1) {
            // alpha maximizing
            for (int i = 1; i <= 6; i++) {
                if (copyboard.moveIsPossible(i)) {
                    GameState copyCurrentBoard = copyboard.clone();
                    copyCurrentBoard.makeMove(i);

                    int currentPlayer = copyCurrentBoard.getNextPlayer();
                    //int currentPlayer = which_player(copyCurrentBoard);

                    int score = minimax_B(copyCurrentBoard, depth - 1, currentPlayer, alpha, beta);


                    if (score > alpha) {
                        alpha = score;

                        if (depth == currentDepth) {
                            bestMove = i;
                        }
                    }

                    // Prune if alpha is greater than beta
                    if(alpha >= beta) {
                        return alpha;
                    }
                }
            }
            return alpha;
        } else {
            // Beta minimizing
            for (int i = 1; i <= 6; i++) {
                if (copyboard.moveIsPossible(i)) {
                    GameState copyCurrentBoard = copyboard.clone();
                    copyCurrentBoard.makeMove(i);

                    int currentPlayer = copyCurrentBoard.getNextPlayer();
                    //int currentPlayer = which_player(copyCurrentBoard);

                    int score = minimax_B(copyCurrentBoard, depth - 1, currentPlayer, alpha, beta);

                    if (score <= beta) {
                        beta = score;
                    }

                    // prune if alpha is greater than beta.
                    if (alpha >= beta) {
                        return beta;
                    }
                }
            }
            return beta;
        }
    }

    public int getMove(GameState currentBoard)
    {
        // Used to play AI vs AI
        if (currentBoard.getNextPlayer() == 1) {
            // Grade B-version
            return calculateBestMove(currentBoard);
        } else {
            // Grade E-version
            return getMoveSec(currentBoard);
        }
    }

    // E-version.
    public int getMoveSec(GameState currentBoard)
    {
        int bestMove = 0;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 1; i<=6; i++)
        {
            System.out.print("Try move " + i + "\n");
            if(currentBoard.moveIsPossible(i))
            {
                GameState newBoard = currentBoard.clone();
                newBoard.makeMove(i);

                boolean playerturn;

                playerturn = newBoard.getNextPlayer() == 2;

                int score = minimax_E(newBoard, 10, playerturn); //
                if(score > bestScore)
                {
                    bestScore = score;
                    bestMove = i;

                    System.out.print("current best move is" + bestMove + "\n");
                }
            }
        }
        return bestMove;
    }

    public int minimax_E(GameState board, int depth, boolean ismaxturn) //GameState board
    {
        boolean playerturn;

        int secondPlayer = 1;
        if (player == 1) {
            secondPlayer = 2;
        }

        if (depth == 0 || board.gameEnded())
        {
            return board.getScore(player) - board.getScore(secondPlayer);
        }

        int bestScore;
        if (ismaxturn)
        {
            bestScore = Integer.MIN_VALUE;

            for (int i = 1; i<=6; i++)
            {
                if(board.moveIsPossible(i))
                {
                    GameState boardRecru = board.clone();
                    boardRecru.makeMove(i);

                    playerturn = boardRecru.getNextPlayer() == 2;

                    int score = minimax_E(boardRecru, depth - 1, playerturn); //

                    if (score > bestScore)
                    {
                        bestScore = score;
                    }
                }
            }
            return bestScore;
        } else {
            bestScore = Integer.MAX_VALUE;
            for (int i = 1; i<=6; i++) {
                if (board.moveIsPossible(i))
                {
                    GameState boardRecru = board.clone();
                    boardRecru.makeMove(i);

                    playerturn = boardRecru.getNextPlayer() == 2;

                    int score = minimax_E(boardRecru, depth - 1, playerturn); //

                    if (score < bestScore) {
                        bestScore = score;
                    }
                }
            }
            return bestScore;
        }
    }
}