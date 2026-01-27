package server.variplex.Auction.SQL;

public interface Database {
    void saveShowpiece(ShowPiece showPiece);

    ShowPiece removeShowpiece(String player, int id, String status);

    void closeConnection();
}