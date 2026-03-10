import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ChessGame extends JFrame {

    // ─── Constants ────────────────────────────────────────────────────────────
    static final int TILE = 80;
    static final Color LIGHT  = new Color(240, 217, 181);
    static final Color DARK   = new Color(181, 136,  99);
    static final Color SELECT = new Color( 20, 160,  20, 160);
    static final Color MOVE   = new Color( 20, 200,  20, 130);
    static final Color CHECK  = new Color(220,  30,  30, 180);

    // ─── Piece codes  (sign = colour: + white, – black) ───────────────────────
    static final int EMPTY = 0, PAWN = 1, KNIGHT = 2, BISHOP = 3,
                     ROOK  = 4, QUEEN = 5, KING   = 6;

    // Unicode chess symbols
    static final String[] SYMBOLS = {
        "\u2659", "\u2658", "\u2657", "\u2656", "\u2655", "\u2654",  // white ♙♘♗♖♕♔
        "\u265F", "\u265E", "\u265D", "\u265C", "\u265B", "\u265A"   // black ♟♞♝♜♛♚
    };

    // ─── Board state ──────────────────────────────────────────────────────────
    int[][] board = new int[8][8];

    // Castling rights
    boolean whiteKingMoved, blackKingMoved;
    boolean whiteRookAMoved, whiteRookHMoved;
    boolean blackRookAMoved, blackRookHMoved;

    // En-passant target square (-1 if none)
    int epCol = -1, epRow = -1;

    boolean whiteTurn = true;

    // Selection
    int selRow = -1, selCol = -1;
    List<int[]> legalMoves = new ArrayList<>();

    // Status
    String status = "White's turn";
    boolean gameOver = false;

    // Move history
    List<String> moveHistory = new ArrayList<>();

    // ─── UI panels ────────────────────────────────────────────────────────────
    BoardPanel boardPanel;
    JLabel statusLabel;
    JTextArea moveLog;

    // ══════════════════════════════════════════════════════════════════════════
    public ChessGame() {
        super("Chess");
        initBoard();
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ─── Initial position ─────────────────────────────────────────────────────
    void initBoard() {
        int[] backRank = {ROOK, KNIGHT, BISHOP, QUEEN, KING, BISHOP, KNIGHT, ROOK};
        for (int c = 0; c < 8; c++) {
            board[0][c] = -backRank[c];   // black back rank
            board[1][c] = -PAWN;          // black pawns
            board[6][c] =  PAWN;          // white pawns
            board[7][c] =  backRank[c];   // white back rank
        }
    }

    // ─── Build UI ─────────────────────────────────────────────────────────────
    void buildUI() {
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(new Color(30, 30, 30));

        boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(8 * TILE, 8 * TILE));
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e); }
        });

        statusLabel = new JLabel(status, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBackground(new Color(50, 50, 50));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        moveLog = new JTextArea(20, 14);
        moveLog.setEditable(false);
        moveLog.setFont(new Font("Courier New", Font.PLAIN, 13));
        moveLog.setBackground(new Color(40, 40, 40));
        moveLog.setForeground(new Color(200, 200, 200));
        moveLog.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JScrollPane scroll = new JScrollPane(moveLog);
        scroll.setPreferredSize(new Dimension(180, 8 * TILE));
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80)),
            "Moves", 0, 0,
            new Font("Georgia", Font.BOLD, 13), Color.LIGHT_GRAY));
        scroll.setBackground(new Color(40, 40, 40));

        JButton newGame = new JButton("New Game");
        newGame.setFont(new Font("Georgia", Font.BOLD, 14));
        newGame.setBackground(new Color(181, 136, 99));
        newGame.setForeground(Color.WHITE);
        newGame.setFocusPainted(false);
        newGame.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        newGame.addActionListener(e -> resetGame());

        JPanel right = new JPanel(new BorderLayout(4, 8));
        right.setBackground(new Color(30, 30, 30));
        right.add(scroll, BorderLayout.CENTER);
        right.add(newGame, BorderLayout.SOUTH);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        add(statusLabel,  BorderLayout.NORTH);
        add(boardPanel,   BorderLayout.CENTER);
        add(right,        BorderLayout.EAST);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
    }

    // ─── Reset ────────────────────────────────────────────────────────────────
    void resetGame() {
        board = new int[8][8];
        whiteKingMoved = blackKingMoved = false;
        whiteRookAMoved = whiteRookHMoved = false;
        blackRookAMoved = blackRookHMoved = false;
        epCol = epRow = -1;
        whiteTurn = true;
        selRow = selCol = -1;
        legalMoves.clear();
        moveHistory.clear();
        moveLog.setText("");
        gameOver = false;
        initBoard();
        setStatus("White's turn");
        boardPanel.repaint();
    }

    // ─── Click handler ────────────────────────────────────────────────────────
    void handleClick(MouseEvent e) {
        if (gameOver) return;
        int col = e.getX() / TILE;
        int row = e.getY() / TILE;
        if (col < 0 || col > 7 || row < 0 || row > 7) return;

        if (selRow == -1) {
            // Select a piece
            int p = board[row][col];
            if (p != EMPTY && (whiteTurn ? p > 0 : p < 0)) {
                selRow = row; selCol = col;
                legalMoves = getLegalMoves(row, col);
            }
        } else {
            // Try to move
            int[] target = {row, col};
            boolean valid = false;
            for (int[] m : legalMoves) {
                if (m[0] == row && m[1] == col) { valid = true; break; }
            }
            if (valid) {
                applyMove(selRow, selCol, row, col);
            } else if (board[row][col] != EMPTY &&
                       (whiteTurn ? board[row][col] > 0 : board[row][col] < 0)) {
                // Re-select
                selRow = row; selCol = col;
                legalMoves = getLegalMoves(row, col);
            } else {
                selRow = selCol = -1;
                legalMoves.clear();
            }
        }
        boardPanel.repaint();
    }

    // ─── Apply a move ─────────────────────────────────────────────────────────
    void applyMove(int fr, int fc, int tr, int tc) {
        int piece = board[fr][fc];
        int target = board[tr][tc];
        String moveName = algebraic(fr, fc, tr, tc, piece, target);

        // En-passant capture
        if (Math.abs(piece) == PAWN && fc != tc && board[tr][tc] == EMPTY) {
            board[fr][tc] = EMPTY; // captured pawn
        }

        // Castling
        if (Math.abs(piece) == KING && Math.abs(fc - tc) == 2) {
            if (tc == 6) { board[tr][5] = board[tr][7]; board[tr][7] = EMPTY; } // king-side
            else         { board[tr][3] = board[tr][0]; board[tr][0] = EMPTY; } // queen-side
        }

        // Update castling flags
        if (Math.abs(piece) == KING) { if (piece > 0) whiteKingMoved = true; else blackKingMoved = true; }
        if (piece ==  ROOK) { if (fc == 0) whiteRookAMoved = true; if (fc == 7) whiteRookHMoved = true; }
        if (piece == -ROOK) { if (fc == 0) blackRookAMoved = true; if (fc == 7) blackRookHMoved = true; }

        // En-passant target update
        epCol = -1; epRow = -1;
        if (Math.abs(piece) == PAWN && Math.abs(fr - tr) == 2) {
            epRow = (fr + tr) / 2; epCol = fc;
        }

        board[tr][tc] = piece;
        board[fr][fc] = EMPTY;

        // Pawn promotion
        if (piece == PAWN  && tr == 0) board[tr][tc] = promoteDialog(1);
        if (piece == -PAWN && tr == 7) board[tr][tc] = -promoteDialog(-1);

        selRow = selCol = -1; legalMoves.clear();
        whiteTurn = !whiteTurn;

        // Log move
        moveHistory.add(moveName);
        updateMoveLog();

        // Check game state
        checkGameState();
    }

    int promoteDialog(int sign) {
        String[] opts = {"Queen", "Rook", "Bishop", "Knight"};
        int r = JOptionPane.showOptionDialog(this, "Promote pawn to:", "Promotion",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
        return sign * (r == 0 ? QUEEN : r == 1 ? ROOK : r == 2 ? BISHOP : KNIGHT);
    }

    // ─── Post-move game-state check ───────────────────────────────────────────
    void checkGameState() {
        boolean inCheck = isInCheck(whiteTurn);
        boolean hasMoves = hasAnyLegalMove(whiteTurn);
        String side = whiteTurn ? "White" : "Black";
        if (!hasMoves) {
            gameOver = true;
            if (inCheck) setStatus("Checkmate! " + (whiteTurn ? "Black" : "White") + " wins! 🏆");
            else         setStatus("Stalemate! It's a draw.");
        } else if (inCheck) {
            setStatus(side + " is in Check!");
        } else {
            setStatus(side + "'s turn");
        }
    }

    void setStatus(String s) {
        status = s;
        statusLabel.setText(s);
    }

    // ─── Move log ─────────────────────────────────────────────────────────────
    void updateMoveLog() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moveHistory.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2 + 1)).append(". ");
            sb.append(moveHistory.get(i)).append("  ");
            if (i % 2 == 1) sb.append("\n");
        }
        moveLog.setText(sb.toString());
        moveLog.setCaretPosition(moveLog.getDocument().getLength());
    }

    // ─── Algebraic notation (simplified) ──────────────────────────────────────
    String algebraic(int fr, int fc, int tr, int tc, int piece, int target) {
        String files = "abcdefgh";
        String from  = "" + files.charAt(fc) + (8 - fr);
        String to    = "" + files.charAt(tc) + (8 - tr);
        String cap   = (target != EMPTY) ? "x" : "";
        if (Math.abs(piece) == PAWN && fc != tc && target == EMPTY) cap = "x"; // en passant
        if (Math.abs(piece) == KING && Math.abs(fc - tc) == 2) return tc == 6 ? "O-O" : "O-O-O";
        String pname = "PNBRQK".charAt(Math.abs(piece) - 1) == 'P' ? "" :
                       String.valueOf("PNBRQK".charAt(Math.abs(piece) - 1));
        return pname + from + cap + to;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MOVE GENERATION
    // ══════════════════════════════════════════════════════════════════════════

    /** Raw moves (may leave king in check) */
    List<int[]> getPseudoMoves(int row, int col) {
        List<int[]> moves = new ArrayList<>();
        int p = board[row][col];
        if (p == EMPTY) return moves;
        int sign = p > 0 ? 1 : -1;

        switch (Math.abs(p)) {
            case PAWN   -> addPawnMoves(moves, row, col, sign);
            case KNIGHT -> addJumps(moves, row, col, sign,
                              new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}});
            case BISHOP -> addRays(moves, row, col, sign, new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}});
            case ROOK   -> addRays(moves, row, col, sign, new int[][]{{-1,0},{1,0},{0,-1},{0,1}});
            case QUEEN  -> addRays(moves, row, col, sign,
                              new int[][]{{-1,-1},{-1,1},{1,-1},{1,1},{-1,0},{1,0},{0,-1},{0,1}});
            case KING   -> {
                addJumps(moves, row, col, sign,
                    new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}});
                addCastling(moves, row, col, sign);
            }
        }
        return moves;
    }

    void addPawnMoves(List<int[]> moves, int r, int c, int sign) {
        int dir = -sign; // white moves up (row decreases)
        int nr = r + dir;
        // Single push
        if (inBounds(nr, c) && board[nr][c] == EMPTY) {
            moves.add(new int[]{nr, c});
            // Double push from starting rank
            int startRank = sign > 0 ? 6 : 1;
            if (r == startRank && board[r + 2 * dir][c] == EMPTY)
                moves.add(new int[]{r + 2 * dir, c});
        }
        // Captures
        for (int dc : new int[]{-1, 1}) {
            int nc = c + dc;
            if (inBounds(nr, nc)) {
                if (board[nr][nc] * sign < 0) moves.add(new int[]{nr, nc}); // enemy
                // En passant
                if (nr == epRow && nc == epCol) moves.add(new int[]{nr, nc});
            }
        }
    }

    void addJumps(List<int[]> moves, int r, int c, int sign, int[][] deltas) {
        for (int[] d : deltas) {
            int nr = r + d[0], nc = c + d[1];
            if (inBounds(nr, nc) && board[nr][nc] * sign <= 0)
                moves.add(new int[]{nr, nc});
        }
    }

    void addRays(List<int[]> moves, int r, int c, int sign, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBounds(nr, nc)) {
                if (board[nr][nc] == EMPTY) moves.add(new int[]{nr, nc});
                else { if (board[nr][nc] * sign < 0) moves.add(new int[]{nr, nc}); break; }
                nr += d[0]; nc += d[1];
            }
        }
    }

    void addCastling(List<int[]> moves, int r, int c, int sign) {
        boolean kingMoved  = sign > 0 ? whiteKingMoved  : blackKingMoved;
        boolean rookAMoved = sign > 0 ? whiteRookAMoved : blackRookAMoved;
        boolean rookHMoved = sign > 0 ? whiteRookHMoved : blackRookHMoved;
        if (kingMoved || isInCheck(sign > 0)) return;
        // King-side
        if (!rookHMoved && board[r][5] == EMPTY && board[r][6] == EMPTY
            && !squareAttacked(r, 5, sign) && !squareAttacked(r, 6, sign))
            moves.add(new int[]{r, 6});
        // Queen-side
        if (!rookAMoved && board[r][3] == EMPTY && board[r][2] == EMPTY && board[r][1] == EMPTY
            && !squareAttacked(r, 3, sign) && !squareAttacked(r, 2, sign))
            moves.add(new int[]{r, 2});
    }

    /** Legal moves = pseudo minus those that leave own king in check */
    List<int[]> getLegalMoves(int r, int c) {
        List<int[]> pseudo = getPseudoMoves(r, c);
        List<int[]> legal  = new ArrayList<>();
        int sign = board[r][c] > 0 ? 1 : -1;
        for (int[] m : pseudo) {
            if (!leavesKingInCheck(r, c, m[0], m[1], sign)) legal.add(m);
        }
        return legal;
    }

    boolean leavesKingInCheck(int fr, int fc, int tr, int tc, int sign) {
        // Make move on a copy
        int[][] copy = copyBoard();
        int savedEpRow = epRow, savedEpCol = epCol;
        // En passant capture
        if (Math.abs(copy[fr][fc]) == PAWN && fc != tc && copy[tr][tc] == EMPTY)
            copy[fr][tc] = EMPTY;
        copy[tr][tc] = copy[fr][fc];
        copy[fr][fc] = EMPTY;
        boolean inCheck = isInCheckBoard(copy, sign > 0);
        return inCheck;
    }

    boolean hasAnyLegalMove(boolean white) {
        int sign = white ? 1 : -1;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] * sign > 0 && !getLegalMoves(r, c).isEmpty())
                    return true;
        return false;
    }

    // ─── Check detection ──────────────────────────────────────────────────────
    boolean isInCheck(boolean white) { return isInCheckBoard(board, white); }

    boolean isInCheckBoard(int[][] b, boolean white) {
        int sign = white ? 1 : -1;
        int kr = -1, kc = -1;
        outer: for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (b[r][c] == sign * KING) { kr = r; kc = c; break outer; }
        if (kr == -1) return false;
        return squareAttackedBoard(b, kr, kc, sign);
    }

    boolean squareAttacked(int r, int c, int sign) { return squareAttackedBoard(board, r, c, sign); }

    boolean squareAttackedBoard(int[][] b, int r, int c, int sign) {
        // Attacked by knights
        for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
            int nr = r+d[0], nc = c+d[1];
            if (inBounds(nr,nc) && b[nr][nc] == -sign * KNIGHT) return true;
        }
        // Attacked by kings
        for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int nr = r+d[0], nc = c+d[1];
            if (inBounds(nr,nc) && b[nr][nc] == -sign * KING) return true;
        }
        // Diagonals (bishop / queen)
        for (int[] d : new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}) {
            int nr = r+d[0], nc = c+d[1];
            while (inBounds(nr,nc)) {
                int p = b[nr][nc];
                if (p != EMPTY) { if (p == -sign*BISHOP || p == -sign*QUEEN) return true; break; }
                nr+=d[0]; nc+=d[1];
            }
        }
        // Straights (rook / queen)
        for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
            int nr = r+d[0], nc = c+d[1];
            while (inBounds(nr,nc)) {
                int p = b[nr][nc];
                if (p != EMPTY) { if (p == -sign*ROOK || p == -sign*QUEEN) return true; break; }
                nr+=d[0]; nc+=d[1];
            }
        }
        // Pawn attacks
        int pawnDir = sign; // enemy pawn attacks from opposite direction
        for (int dc : new int[]{-1,1}) {
            int nr = r + pawnDir, nc = c + dc;
            if (inBounds(nr,nc) && b[nr][nc] == -sign*PAWN) return true;
        }
        return false;
    }

    // ─── Utilities ────────────────────────────────────────────────────────────
    boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    int[][] copyBoard() {
        int[][] copy = new int[8][8];
        for (int r = 0; r < 8; r++) copy[r] = board[r].clone();
        return copy;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOARD PANEL
    // ══════════════════════════════════════════════════════════════════════════
    class BoardPanel extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw squares
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    g2.setColor((r + c) % 2 == 0 ? LIGHT : DARK);
                    g2.fillRect(c * TILE, r * TILE, TILE, TILE);
                }
            }

            // Highlight selected
            if (selRow != -1) {
                g2.setColor(SELECT);
                g2.fillRect(selCol * TILE, selRow * TILE, TILE, TILE);
            }

            // Highlight legal moves
            for (int[] m : legalMoves) {
                g2.setColor(MOVE);
                if (board[m[0]][m[1]] != EMPTY) {
                    // Capture hint — ring
                    g2.setStroke(new BasicStroke(5));
                    g2.drawOval(m[1]*TILE+4, m[0]*TILE+4, TILE-8, TILE-8);
                    g2.setStroke(new BasicStroke(1));
                } else {
                    // Dot
                    int d = TILE / 3;
                    g2.fillOval(m[1]*TILE + d, m[0]*TILE + d, d, d);
                }
            }

            // Highlight king in check
            if (!gameOver && isInCheck(whiteTurn)) {
                // Find king
                int sign = whiteTurn ? 1 : -1;
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        if (board[r][c] == sign * KING) {
                            g2.setColor(CHECK);
                            g2.fillRect(c * TILE, r * TILE, TILE, TILE);
                        }
            }

            // Coordinate labels
            g2.setFont(new Font("Georgia", Font.BOLD, 11));
            String files = "abcdefgh";
            for (int i = 0; i < 8; i++) {
                g2.setColor((i % 2 == 0) ? DARK : LIGHT);
                g2.drawString(String.valueOf(8 - i), 2, i * TILE + 13);
                g2.setColor((i % 2 == 0) ? LIGHT : DARK);
                g2.drawString(String.valueOf(files.charAt(i)), i * TILE + TILE - 12, 8 * TILE - 2);
            }

            // Draw pieces
            g2.setFont(new Font("Serif", Font.PLAIN, (int)(TILE * 0.78)));
            FontMetrics fm = g2.getFontMetrics();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    int p = board[r][c];
                    if (p == EMPTY) continue;
                    int idx = Math.abs(p) - 1 + (p < 0 ? 6 : 0);
                    String sym = SYMBOLS[idx];
                    int x = c * TILE + (TILE - fm.stringWidth(sym)) / 2;
                    int y = r * TILE + fm.getAscent() - (int)(TILE * 0.06);
                    // Shadow
                    g2.setColor(new Color(0,0,0,80));
                    g2.drawString(sym, x+2, y+2);
                    // Piece
                    g2.setColor(p > 0 ? Color.WHITE : new Color(20, 20, 20));
                    g2.drawString(sym, x, y);
                }
            }

            // Thin border
            g2.setColor(new Color(60, 40, 20));
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(0, 0, 8*TILE-1, 8*TILE-1);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGame::new);
    }
}
