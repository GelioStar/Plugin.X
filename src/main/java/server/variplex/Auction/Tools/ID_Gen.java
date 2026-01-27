package server.variplex.Auction.Tools;

import java.util.concurrent.ThreadLocalRandom;

public class ID_Gen {
// Метод generateID_M1()
//    private static final long TIME_MASK = 0x3FFFFFFFL; // 30 бит (максимум ~34 года)
//    private static final long RAND_MASK = 0x7FFL;      // 11 бит (2048 значений/мс)
//    private static final int BASE = 36;
// Метод generateID_M2() и generateID_M3()
    private static final char[] ALPHABET = "0123456789abcdefghijkmnpqrstuvwxyz".toCharArray();
    private static final int ALPHABET_SIZE = ALPHABET.length;
    private static final long TIME_BITS = 30; // ~34 года уникальности
    private static final long RAND_BITS = 12; // 4096 значений/мс

//    public static String generateID_M1() {
//        long time = System.currentTimeMillis();
//        long rand = ThreadLocalRandom.current().nextLong();
//        long value = ((time & TIME_MASK) << 11) | (rand & RAND_MASK);
//        int d7 = (int) (value % BASE); value /= BASE;
//        int d6 = (int) (value % BASE); value /= BASE;
//        int d5 = (int) (value % BASE); value /= BASE;
//        int d4 = (int) (value % BASE); value /= BASE;
//        int d3 = (int) (value % BASE); value /= BASE;
//        int d2 = (int) (value % BASE); value /= BASE;
//        int d1 = (int) (value % BASE); value /= BASE;
//        int d0 = (int) (value % BASE);
//        return new String( new char[] {toChar(d0), toChar(d1), toChar(d2), toChar(d3), toChar(d4), toChar(d5), toChar(d6), toChar(d7)} );
//    }
//    private static char toChar(int digit) { return (char) (digit < 10 ? '0' + digit : 'a' + digit - 10); }

    public static String generateID_M2() {
        // Комбинируем время и случайность
        long timestampPart = (System.currentTimeMillis() & ((1L << TIME_BITS) - 1)) << RAND_BITS;
        long randomPart = ThreadLocalRandom.current().nextLong() & ((1L << RAND_BITS) - 1);
        long combined = timestampPart | randomPart;
        // Преобразуем в base32 (оптимально для 8 символов)
        char[] id = new char[8];
        for (int i = 7; i >= 0; i--) { id[i] = ALPHABET[(int) (combined % ALPHABET_SIZE)]; combined /= ALPHABET_SIZE; }
        return new String(id);
    }
/*    public static String generateID_M3() {
        long combined = ((System.currentTimeMillis() << RAND_BITS) | (ThreadLocalRandom.current().nextLong() & ((1L << RAND_BITS) - 1)));
        return new String(new char[] {
                ALPHABET[(int) ((combined >>> 35) % ALPHABET_SIZE)],
                ALPHABET[(int) ((combined >>> 30) % ALPHABET_SIZE)],
                ALPHABET[(int) ((combined >>> 25) % ALPHABET_SIZE)],
                ALPHABET[(int) ((combined >>> 20) % ALPHABET_SIZE)],
                ALPHABET[(int) ((combined >>> 15) % ALPHABET_SIZE)],
                ALPHABET[(int) ((combined >>> 10) % ALPHABET_SIZE)],
                ALPHABET[(int) ((combined >>> 5) % ALPHABET_SIZE)],
                ALPHABET[(int) (combined % ALPHABET_SIZE)]
        });
    }*/
}