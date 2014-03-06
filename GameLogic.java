import java.util.Date;
import java.util.concurrent.*;

public class GameLogic implements IGameLogic {
    private int _cols = 0;
    private int _rows = 0;
    private GameState _state;
    private int _playerId;
    private int _otherPlayerId;
    private int _cutoff = 10;
    private boolean DEBUG = true;
    private int MAX_DECISION_TIME_MS = 9900;

    public GameLogic() {
        //TODO Write your implementation for this method
    }

    /**
     * Creates a new empty game board of the specified dimensions and
     * indicates the ID of the player.
     * @param x rows
     * @param y columns
     * @param playerId 1 = blue (player1), 2 = red (player2)
     */
    public void initializeGame(int x, int y, int playerId) {
        _cols = x;
        _rows = y;
        _playerId = playerId;
        _otherPlayerId = playerId == 1 ? 2 : 1;
        _state = new GameState(x, y);
    }

    /**
     * Checks if any of the two players have 4-in-a-row.
     * @return
     */
    public Winner gameFinished() {
        for(int c = 0; c < _cols; c++) {
            if(connectedNeightboors(c) >= 4) {
                connectedNeightboors(c);
                int row = _state.getCoinsInColumn(c) - 1;
                int playerId = _state.getCoinPlayer(c, row);
                return playerId == 1 ? Winner.PLAYER1 : Winner.PLAYER2;
            }
            if(_state.isBoardFull())
                return Winner.TIE;
        }

        return Winner.NOT_FINISHED;
    }

    private int connectedNeightboors(int column) {
        int row = _state.getCoinsInColumn(column) - 1;
        if(row < 0)
            return 0;
        int player = _state.getCoinPlayer(column, row);

        return connectedNeightboors(column, row, player);
    }

    private int connectedNeightboors(int c, int r, int playerId) {
        if(_state.getCoinPlayer(c,r) != playerId)
            return 0;

        int count = 0;
        int deepestRow = Math.max(0, r - 3);

        // Horizontal
        int tmp = 0;
        for(int i = c+1 ;i < _cols && _state.getCoinPlayer(i, r) == playerId; i++)
            tmp++;
        for(int i = c-1; i >= 0 && _state.getCoinPlayer(i, r) == playerId; i--)
            tmp++;
        count = Math.max(tmp, count);

        // Vertical
        tmp = 0;
        for(int j = r-1; j >= deepestRow && _state.getCoinPlayer(c, j) == playerId; j--)
            tmp++;
        count = Math.max(tmp, count);

        // Diagonal left
        tmp = 0;
        for(int i = c-1, j = r-1; i >= 0 && j >= deepestRow && _state.getCoinPlayer(i, j) == playerId; i--, j--)
            tmp++;
        count = Math.max(tmp, count);

        // Diagonal right
        tmp = 0;
        for(int i = c+1, j = r-1; i < _cols && j >= deepestRow && _state.getCoinPlayer(i, j) == playerId; i++, j--)
            tmp++;
        count = Math.max(tmp, count);

        return 1 + count;
    }

    private int totalConnectedNeightboors(int column) {
        int row = _state.getCoinsInColumn(column) - 1;
        if(row < 0)
            return 0;
        int player = _state.getCoinPlayer(column, row);

        return totalConnectedNeightboors(column, row, player);
    }

    private int totalConnectedNeightboors(int c, int r, int playerId) {
        if(_state.getCoinPlayer(c,r) != playerId)
            return 0;

        int count = 1;
        int deepestRow = Math.max(0, r - 3);

        // Horizontal
        for(int i = c+1 ;i < _cols && _state.getCoinPlayer(i, r) == playerId; i++)
            count++;
        for(int i = c-1; i >= 0 && _state.getCoinPlayer(i, r) == playerId; i--)
            count++;

        // Vertical
        for(int j = r-1; j >= deepestRow && _state.getCoinPlayer(c, j) == playerId; j--)
            count++;

        // Diagonal left
        for(int i = c-1, j = r-1; i >= 0 && j >= deepestRow && _state.getCoinPlayer(i, j) == playerId; i--, j--)
            count++;

        // Diagonal right
        for(int i = c+1, j = r-1; i < _cols && j >= deepestRow && _state.getCoinPlayer(i, j) == playerId; i++, j--)
            count++;

        return count;
    }

    /**
     * Notifies that a token/coin is put in the specified column of the
     * game board.
     * @param column The column where the coin is inserted.
     * @param playerId The ID of the current player.
     */
    public void insertCoin(int column, int playerId) {
        _state.addCoin(column, playerId);
        _state.resetUndoStack();
        _state.printBoard();
    }

    /**
     * Calculates the next move This is where you should implement/call
     * your heuristic evaluation functions etc.
     * @return
     */
    public int decideNextMove() {
        long startTime = new Date().getTime();
        int result = 0;
        _cutoff = 1;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = null;

        System.out.print("Cutoff: 0");

        try {
            while(true) // while(We have time left)
            {
                long msLeft = MAX_DECISION_TIME_MS - (new Date().getTime() - startTime);

                if(DEBUG) System.out.format("\rCutoff: %d", _cutoff);

                future = executor.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return minimax();
                    }
                });

                result = future.get(msLeft, TimeUnit.MILLISECONDS);
                _cutoff++;
            }
        } catch (InterruptedException ignored) {
        } catch (ExecutionException ignored) {
        } catch (TimeoutException ignored) {
            // Stop the execution of minimax.
            future.cancel(true);
            try {
                future.get();
            } catch (Exception ignore) {
            }
        }

        _state.undoAll();
        if(DEBUG) System.out.format("\r\nDecision: %d\r\n", result);
        return result;
    }

    private double boardEvaluation(int playerId) {
        int own = 0;
        int enemy = 0;

        for(int c = 0; c < _cols; c++)
            if(0 < _state.getCoinsInColumn(c)) {
                int cns = totalConnectedNeightboors(c);
                if(_state.getCoinPlayer(c) == playerId)
                    own += cns;
                else
                    enemy += cns;
            }

        return (own - enemy) / 10000.0;
    }

    private int minimax() throws InterruptedException {
        // Loop through potential actions (columns)
        double greatestGain = Double.NEGATIVE_INFINITY;
        int columnToPlay = -1;
        for(int c = 0; c < _cols; c++)
            if(_state.getCoinsInColumn(c) < _rows)
            {
                _state.addCoin(c, _playerId);
                double gain = min(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0);
                _state.undoAll();

                if(_cutoff == 1 && gain == 1)
                    return c;

                if(greatestGain < gain) {
                    columnToPlay = c;
                    greatestGain = gain;
                }
            }

        return columnToPlay;
    }

    private double min(double a, double b, int depth) throws InterruptedException {
        if(Thread.currentThread().isInterrupted())
            throw new InterruptedException();

        Winner winner = gameFinished();
        if(winner != Winner.NOT_FINISHED)
            return whoWon(winner);

        if (depth > _cutoff)
            return boardEvaluation(_otherPlayerId);

        depth++;

        double result = Double.POSITIVE_INFINITY;
        for(int c = 0; c < _cols; c++)
            if(_state.getCoinsInColumn(c) < _rows)
            {
                _state.addCoin(c, _otherPlayerId);
                result = Math.min(max(a, b, depth), result);
                _state.undoAddCoin();
                if (result <= a)
                    return result;
                b = Math.min(b, result);
            }

        return result;
    }

    private double max(double a, double b, int depth) throws InterruptedException {
        if(Thread.currentThread().isInterrupted())
            throw new InterruptedException();

        Winner winner = gameFinished();
        if(winner != Winner.NOT_FINISHED)
            return whoWon(winner);

        if (depth > _cutoff)
            return boardEvaluation(_playerId);

        depth++;

        double result = Double.NEGATIVE_INFINITY;
        for(int c = 0; c < _cols; c++)
            if(_state.getCoinsInColumn(c) < _rows)
            {
                _state.addCoin(c, _playerId);
                result = Math.max(min(a, b, depth), result);
                _state.undoAddCoin();
                if (result >= b)
                    return result;
                a = Math.max(a, result);
            }

        return result;
    }

    private int whoWon(Winner winner)
    {
        if(winner == Winner.TIE)
            return 0;
        else if (winner == Winner.PLAYER1 && _playerId == 1)
            return 1;
        else if (winner == Winner.PLAYER2 && _playerId == 2)
            return 1;
        return -1;
    }
}
