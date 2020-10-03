package com.bradyrussell.uiscoin.blockchain;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.block.BlockHeader;
import com.bradyrussell.uiscoin.transaction.Transaction;
import com.bradyrussell.uiscoin.transaction.TransactionInput;
import com.bradyrussell.uiscoin.transaction.TransactionOutput;
import com.bradyrussell.uiscoin.transaction.TransactionOutputBuilder;

import java.util.*;
import java.util.logging.Logger;

public abstract class BlockChainStorageBase {
    private static final Logger Log = Logger.getLogger(BlockChainStorageBase.class.getName());

    public static final String BlocksDatabase = "blocks";
    public static final String BlockHeadersDatabase = "headers";
    public static final String TransactionToBlockDatabase = "transaction_to_block";
    public static final String TransactionOutputDatabase = "unspent_transaction_outputs";

    public int BlockHeight = -1;
    public byte[] HighestBlockHash = null;

    public abstract boolean open();
    public abstract void close();

    public abstract void addToMempool(Transaction t);
    public abstract void removeFromMempool(Transaction t);
    public abstract List<Transaction> getMempool();

   // public abstract byte[] get(byte[] Key);
    public abstract byte[] get(byte[] Key, String Database);
    // public abstract void get(byte[] Key, byte[] Value);
    //public abstract void put(byte[] Key, byte[] Value);
    public abstract void put(byte[] Key, byte[] Value, String Database);
    public abstract void remove(byte[] Key, String Database);
    public abstract boolean exists(byte[] Key, String Database);
    public abstract List<byte[]> keys(String Database);

    public Block getBlock(byte[] BlockHash) {
        Log.info("BlockHash = " + Arrays.toString(BlockHash));

        Block block = new Block();
        block.setBinaryData(get(BlockHash, BlocksDatabase));
        return block;
    }

    public BlockHeader getBlockHeader(byte[] BlockHash) {
        Log.info("BlockHash = " + Arrays.toString(BlockHash));

        BlockHeader blockHeader = new BlockHeader();
        byte[] data = get(BlockHash, BlockHeadersDatabase);
        if(data == null) return null;
        blockHeader.setBinaryData(data);
        return blockHeader;
    }

    public Transaction getTransaction(byte[] TransactionHash){
        Log.info("TransactionHash = " + Arrays.toString(TransactionHash));

        Block block = getBlockWithTransaction(TransactionHash);
        for(Transaction transaction:block.Transactions){
            if(Arrays.equals(transaction.getHash(), TransactionHash)) return transaction;
        }
        return null;
    }

    public Block getBlockWithTransaction(byte[] TransactionHash){
        Log.info("TransactionHash = " + Arrays.toString(TransactionHash));

        return getBlock(get(TransactionHash, TransactionToBlockDatabase));
    }

    public TransactionOutput getTransactionOutput(byte[] TransactionHash, int Index){
        Log.info("TransactionHash = " + Arrays.toString(TransactionHash) + ", Index = " + Index);

        TransactionOutput unspentTransactionOutput = getUnspentTransactionOutput(TransactionHash, Index);
        if(unspentTransactionOutput != null) return unspentTransactionOutput;
        return getTransaction(TransactionHash).Outputs.get(Index);
    }

    public TransactionOutput getUnspentTransactionOutput(byte[] TransactionHash, int Index){
        Log.info("TransactionHash = " + Arrays.toString(TransactionHash) + ", Index = " + Index);

        if(TransactionHash == null) return null;
        TransactionOutput transactionOutput = new TransactionOutput();
        byte[] binaryData = get(Util.ConcatArray(TransactionHash, Util.NumberToByteArray(Index)), TransactionOutputDatabase);
        if(binaryData == null) return null;
        transactionOutput.setBinaryData(binaryData);

        return transactionOutput;
    }

    public void putBlock(Block block) {
        put(block.Header.getHash(), block.getBinaryData(), BlocksDatabase);
        putBlockHeader(block);
        ArrayList<Transaction> transactions = block.Transactions;
        for (int TransactionIndex = 0; TransactionIndex < transactions.size(); TransactionIndex++) {
            Transaction transaction = transactions.get(TransactionIndex);

            put(transaction.getHash(), block.Header.getHash(), TransactionToBlockDatabase);

            if(TransactionIndex != 0) { // only for non coinbase transactions
                BlockChain.get().removeFromMempool(transaction); // remove from mempool
                for (TransactionInput input : transaction.Inputs) { //spend input UTXOs
                    removeUnspentTransactionOutput(input.InputHash, input.IndexNumber);
                }
            }

            ArrayList<TransactionOutput> outputs = transaction.Outputs;
            for (int i = 0; i < outputs.size(); i++) {
                putUnspentTransactionOutput(transaction.getHash(), i, outputs.get(i));
            }
        }
    }

    public void putBlockHeader(BlockHeader header, byte[] BlockHash) {
        if(BlockHeight < header.BlockHeight) {
            BlockHeight = header.BlockHeight;
            HighestBlockHash = BlockHash;
        }
        put(BlockHash, header.getBinaryData(), BlockHeadersDatabase);
    }

    public void putBlockHeader(Block block) {
        if(BlockHeight < block.Header.BlockHeight) {
            BlockHeight = block.Header.BlockHeight;
            HighestBlockHash = block.Header.getHash();
        }
        put(block.Header.getHash(), block.Header.getBinaryData(), BlockHeadersDatabase);
    }

    public void putUnspentTransactionOutput(byte[] TransactionHash, int Index, TransactionOutput transactionOutput){
        put(Util.ConcatArray(TransactionHash,Util.NumberToByteArray(Index)), transactionOutput.getBinaryData(), TransactionOutputDatabase);
    }

    public void removeUnspentTransactionOutput(byte[] TransactionHash, int Index){
        remove(Util.ConcatArray(TransactionHash,Util.NumberToByteArray(Index)), TransactionOutputDatabase);
    }

    //this returns UTXO, which are 64 bytes TransactionHash, 4 bytes Index
    public ArrayList<byte[]> ScanUnspentOutputsToAddress(byte[] PublicKeyHash) {
        ArrayList<byte[]> utxo = new ArrayList<>();
        byte[] lockingScript = new TransactionOutputBuilder().setPayToPublicKeyHash(PublicKeyHash).get().LockingScript;

        for(byte[] UTXOHash:keys(TransactionOutputDatabase)){
            TransactionOutput output = new TransactionOutput();
            output.setBinaryData(get(UTXOHash,TransactionOutputDatabase));

            if(Arrays.equals(output.LockingScript,lockingScript)) {
                utxo.add(UTXOHash);
            }
        }
        return utxo;
    }

    public Block getBlockByHeight(int BlockHeight){
        if(BlockHeight < 0) return null;
        byte[] currentBlockHash = HighestBlockHash;
        while(getBlockHeader(currentBlockHash).BlockHeight != BlockHeight) {
            currentBlockHash = getBlockHeader(currentBlockHash).HashPreviousBlock;
        }
        return getBlock(currentBlockHash);
    }

    public List<Block> getBlockChainFromHeight(int BlockHeight){
        if(this.BlockHeight < BlockHeight) return new ArrayList<>();
        if(HighestBlockHash == null) return new ArrayList<>();
        if(BlockHeight < 0) return new ArrayList<>();
        byte[] currentBlockHash = HighestBlockHash;

        List<Block> blockchain = new ArrayList<>();

       // System.out.println("GetBlockChainFromHeight: CurrentBlockHash = "+Util.Base64Encode(currentBlockHash));

        while(getBlockHeader(currentBlockHash) != null && getBlockHeader(currentBlockHash).BlockHeight >= BlockHeight) {
            blockchain.add(getBlock(currentBlockHash));
            currentBlockHash = getBlockHeader(currentBlockHash).HashPreviousBlock;
        }

        Collections.reverse(blockchain);
        return blockchain;
    }

    // merkle root the entire blockchain from BlockHeight
    public byte[] getBlockChainMerkleRoot(int BlockHeight){
        List<byte[]> hashes = new ArrayList<>();

        for(Block block:getBlockChainFromHeight(BlockHeight)){
            hashes.add(block.Header.getHash());
        }

        while(hashes.size() > 1) {
            hashes = Block.MerkleRootStep(hashes);
        }

        return hashes.get(0);
    }
}
