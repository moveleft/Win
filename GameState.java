import java.util.Stack;

public class GameState {
    private static final boolean DEBUG = true;
    private int _cols, _rows;
    private long _board;
    private long _boardCoinColors;
    private int[] _coinsCountPerColumn;
    private Stack<Integer> _moves;

    public GameState(int cols, int rows) {
        _cols = cols;
        _rows = rows;

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

        long bla = 1L << location;
        long blu = _board | (1L << location);
        _board |= (1L << location); // put coin on board
        if (playerId == 2) 
        {
            _boardCoinColors |= (1L << location); // set coin color to 1
        } else {
            _boardCoinColors &= ~(1L << location); // set coin color to 0
        }

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
        _board &= ~(1L << location); // set location to 0
        _boardCoinColors &= ~(1L << location); // set location to 0

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
        if ((_board & (1L << (row*_cols+column) )) != 0)
        {
            // The bit was set
            if ((_boardCoinColors & (1L << (row*_cols+column) )) != 0)
            {
                // The bit was set
                return 2;
            }
            return 1;
        }
        return 0;
    }

    public boolean isBoardFull() {
        for(int c = 0; c < _cols; c++){
            if(_coinsCountPerColumn[c] < _rows)
                return false;
        }
        return true;
    }

    public int getColumnOfPreviouslyPlacedCoin() {
        if(_moves.empty())
            return -1;
        return _moves.peek() % _cols;
    }

    public void resetUndoStack() {
        _moves.clear();
    }

    public int getCoinPlayer(int c) {
        return getCoinPlayer(c, getCoinsInColumn(c) - 1);
    }

    /**
     * Does not guarantee no collisions with board of 7*6,
     * as this required combining two 42 bit numbers into one 64 number.
     * @return The hash of the board.
     */
    public long getBoardHash()
    {
        return (_board << 21 ) ^ _boardCoinColors;
    }
}
