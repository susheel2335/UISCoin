package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.blockchain.BlockChain;
import com.bradyrussell.uiscoin.blockchain.BlockChainStorageBase;
import com.bradyrussell.uiscoin.node.BlockHeaderResponse;
import com.bradyrussell.uiscoin.node.Node;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

public class NodeP2PReceiveBlockHeaderResponseHandler extends SimpleChannelInboundHandler<BlockHeaderResponse> {
    private static final Logger Log = Logger.getLogger(NodeP2PReceiveBlockHeaderResponseHandler.class.getName());

    Node thisNode;

    public NodeP2PReceiveBlockHeaderResponseHandler(Node thisNode) {
        this.thisNode = thisNode;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //System.err.printf("Factorial of %,d is: %,d%n", lastMultiplier, factorial);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, BlockHeaderResponse blockHeaderResponse) throws Exception {
        Log.info("Handler Received block header "+ Util.Base64Encode(blockHeaderResponse.BlockHash));

        if(BlockChain.get().exists(blockHeaderResponse.BlockHash, BlockChainStorageBase.BlockHeadersDatabase)){
            Log.info("Already have. Discarding...");
            return;
        }

        if(!blockHeaderResponse.blockHeader.Verify()) {
            Log.info("Invalid blockheader! Discarding.");
            return;
        }

        Log.info("Storing blockheader...");
        BlockChain.get().putBlockHeader(blockHeaderResponse.blockHeader);

    }
}
