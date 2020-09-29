package com.bradyrussell.uiscoin.netty;

import com.bradyrussell.uiscoin.MagicBytes;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.node.PeerPacketBuilder;
import com.bradyrussell.uiscoin.node.PeerPacketType;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.net.InetAddress;
import java.util.List;

public class NodeP2PMessageDecoder extends ReplayingDecoder<Void>{
/*
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new PeerPacketBuilder(8).putPing(1));
    }
*/

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        System.out.println("1 Decode");
        PeerPacketType packetType = PeerPacketType.getByHeader(byteBuf.readByte());
        System.out.println("2 Decoding "+packetType+" packet.");

        if (packetType != null) {
            switch (packetType){
                case DISCONNECT -> {
                    System.out.println("3 Received disconnect!");
                    channelHandlerContext.writeAndFlush(new PeerPacketBuilder(2).putDisconnect().get());
                    channelHandlerContext.disconnect().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            System.out.println("Disconnected");
                        }
                    });
                    list.add(true);
                }
                case GREETING -> {
                    System.out.println("3 Received greeting!");
                    int Version = byteBuf.readInt();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        System.out.println("Protocol version mismatch, disconnecting!");
                        channelHandlerContext.disconnect();
                        list.add(true);
                        return;
                    }

                    ByteBuf wrappedBuffer = Unpooled.wrappedBuffer(new PeerPacketBuilder(5).putHandshake(1).get());
                    ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(wrappedBuffer);
                    //wrappedBuffer.release();
                    channelFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            if(!channelFuture.isSuccess())
                                channelFuture.cause().printStackTrace();
                        }
                    });

                    list.add(true);
                }
                case HANDSHAKE -> {
                    System.out.println("3 Received handshake!");
                    int Version = byteBuf.readInt();
                    if(Version != MagicBytes.ProtocolVersion.Value) {
                        channelHandlerContext.disconnect();
                        list.add(true);
                        return;
                    }
                    list.add(true);
                }
                case PING -> {
                    System.out.println("3 Received ping!");
                    list.add(true);
                }
                case PEER -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    InetAddress address = InetAddress.getByAddress(Bytes);
                    // Node.addPeer()

                    System.out.println("3 Received peer "+Util.Base64Encode(Bytes));
                    list.add(address);
                }
                case TRANSACTION -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Transaction transaction = new Transaction();
                    transaction.setBinaryData(Bytes);

                    System.out.println("3 Received transaction "+Util.Base64Encode(transaction.getHash()));
                    list.add(transaction);
                }
                case BLOCK -> {
                    int Size = byteBuf.readInt();
                    byte[] Bytes = new byte[Size];
                    byteBuf.readBytes(Bytes);

                    Block block = new Block();
                    block.setBinaryData(Bytes);

                    System.out.println("3 Received block "+Util.Base64Encode(block.getHash()));
                    list.add(block);
                }
            }
        }
    }
}
