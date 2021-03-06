package com.bradyrussell.uiscoin.transaction;

import com.bradyrussell.uiscoin.blockchain.exception.NoSuchBlockException;
import com.bradyrussell.uiscoin.blockchain.exception.NoSuchTransactionException;

import java.util.logging.Logger;

public class TransactionBuilder {
    private static final Logger Log = Logger.getLogger(TransactionBuilder.class.getName());
    Transaction transaction = new Transaction();

    public TransactionBuilder setVersion(int Version){
        transaction.Version = Version;
        return this;
    }

    public TransactionBuilder setLockTime(long LockTime){
        transaction.TimeStamp = LockTime;
        return this;
    }

    public TransactionBuilder addInput(TransactionInput transactionInput){
        transaction.addInput(transactionInput);
        return this;
    }

    public TransactionBuilder addOutput(TransactionOutput transactionOutput){
        transaction.addOutput(transactionOutput);
        Log.info("Added output to transaction for "+transactionOutput.Amount+" satoshis.");
        return this;
    }

/*    @Deprecated // this is not consistent, everything else takes PubKeyHash
    public TransactionBuilder addChangeOutput(byte[] FullAddress, long FeeToLeave){
        long Amount = (transaction.getInputTotal() - transaction.getOutputTotal()) - FeeToLeave;

        assert Amount > 0;

        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(FullAddress);
        transaction.addOutput(new TransactionOutputBuilder().setPayToPublicKeyHash(decodedAddress.PublicKeyHash).setAmount(Amount).get());
        return this;
    }*/

    public TransactionBuilder addChangeOutputToPublicKeyHash(byte[] PublicKeyHash, long FeeToLeave) throws NoSuchTransactionException, NoSuchBlockException {
        long Amount = (transaction.getInputTotal() - transaction.getOutputTotal()) - FeeToLeave;

        if(Amount < 0) {
            Log.warning("Insufficient inputs for this transaction! Input: "+transaction.getInputTotal()+" Output: "+transaction.getOutputTotal()+" Fee: "+FeeToLeave);
        }
        if(Amount == 0) {
            Log.info("There is no extra change for this transaction.");
            return this;
        }
        assert Amount > 0;

        transaction.addOutput(new TransactionOutputBuilder().setPayToPublicKeyHash(PublicKeyHash).setAmount(Amount).get());
        Log.info("Added change output to transaction for "+Amount+" satoshis.");
        return this;
    }

    public Transaction get() {
        return transaction;
    }
}
