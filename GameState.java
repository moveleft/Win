import java.util.Stack;

public class GameState {
    private static final boolean DEBUG = true;
    private int _cols, _rows;
    private int[] _board;
    private int[] _coinsCountPerColumn;
    private Stack<Integer> _moves;

    public GameState(int cols, int rows) {
        _cols = cols;
        _rows = rows;

        _board = new int[cols*rows];
        _coinsCountPerColumn = new int[cols];
        _moves = new Stack<Integer>();
    }

    public void addCoin(int column, int playerId) {
        int coinCount = getCoinsInColumn(column);
        if(coinCount >= _rows) {
            if(DEBUG) printBoard();
            throw new IllegalArgumentException("No room for more coins in the column.");
        }

        _coinsCountPerColumn[column] = coinCount + 1;

        int location = coinCount*_cols + column;

        _board[location] = playerId;

        _moves.push(location);
    }

    public void printBoard() {
        for(int c = 0; c < _cols; c++)
            System.out.format("%d ", getCoinsInColumn(c));
        System.out.println();
        for(int c = 0; c < _cols; c++)
            System.out.format("==", getCoinsInColumn(c));
        System.out.println();

        for(int r = 0; r < _rows; r++) {
            for(int c = 0; c < _cols; c++)
                System.out.format("%d ", getCoinPlayer(c, r));
            System.out.println();
        }
    }

    public void undoAddCoin() {
        if(_moves.empty())
            throw new IllegalStateException("Nothing to undo.");
        int location = _moves.pop();
        _board[location] = 0;

        int c = location % _cols;
        _coinsCountPerColumn[c] = _coinsCountPerColumn[c] - 1;
    }

    public void undoAll() {
        while(!_moves.empty())
            undoAddCoin();
    }

    public int getCoinsInColumn(int column) {
        if(column < 0 || column >= _cols)
            throw new IllegalArgumentException("column out of range.");
        return _coinsCountPerColumn[column];
    }

    public int getCoinPlayer(int column, int row) {
        return _board[row*_cols+column];
    }

    public boolean isBoardFull() {
        for(int c = 0; c < _cols; c++){
            if(_coinsCountPerColumn[c] < _rows)
                return false;
        }

        return true;
    }

    public void resetUndoStack() {
        _moves.clear();
    }

    public int getCoinPlayer(int c) {
        return getCoinPlayer(c, getCoinsInColumn(c) - 1);
    }
}
