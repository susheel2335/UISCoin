package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class Util {
    public static void printBytesReadable(byte[] bytes) {
        System.out.print("[");
        for (byte b : bytes) {

            if (b >= 32 && b <= 126) {
                System.out.print((char) b);
            } else {
                System.out.print("0x");
                System.out.printf("%02X", b);
            }
            System.out.print(" ");
        }
        System.out.println("]");
    }

    public static void printBytesHex(byte[] bytes) {
        System.out.print("[");
        for (byte b : bytes) {
            System.out.print("0x");
            System.out.printf("%02X", b);
            System.out.print(" ");
        }
        System.out.println("]");
    }

    public static byte[] ConcatArray(byte[] A, byte[] B) {
        byte[] C = new byte[A.length + B.length];
        for (int i = 0; i < A.length + B.length; i++) {
            C[i] = (i < A.length) ? A[i] : B[i - A.length];
        }
        return C;
    }

    public static int ByteArrayToNumber(byte[] Bytes) {
        int n = 0;

        if (Bytes.length > 0) n |= Bytes[0] << 24;
        if (Bytes.length > 1) n |= (Bytes[1] & 0xFF) << 16;
        if (Bytes.length > 2) n |= (Bytes[2] & 0xFF) << 8;
        if (Bytes.length > 3) n |= (Bytes[3] & 0xFF);

        return n;
    }

    public static byte[] NumberToByteArray(int Number) {
        return new byte[]{(byte) (Number >> 24), (byte) (Number >> 16), (byte) (Number >> 8), (byte) Number};
    }

    public static String Base64Encode(byte[] Data) {
        return Base64.getUrlEncoder().encodeToString(Data);
    }

    public static byte[] Base64Decode(String Base64String) {
        return Base64.getUrlDecoder().decode(Base64String);
    }

    public static boolean doTransactionsContainTXO(byte[] TransactionHash, int Index, List<Transaction> Transactions) {
        for (Transaction transaction : Transactions) {
            for (TransactionInput input : transaction.Inputs) {
                if (Arrays.equals(input.InputHash, TransactionHash) && input.IndexNumber == Index) return true;
            }
        }
        return false;
    }

    public static String getConstantSalt() {
        return "_UISCoin1.0_salted_password";
    }
}
