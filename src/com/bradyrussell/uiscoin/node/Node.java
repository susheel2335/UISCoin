package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.MagicNumbers;
import com.bradyrussell.uiscoin.Util;
import com.bradyrussell.uiscoin.block.Block;
import com.bradyrussell.uiscoin.netty.ClientHandler;
import com.bradyrussell.uiscoin.netty.NodeP2PClientInitializer;
import com.bradyrussell.uiscoin.netty.NodeP2PServerInitializer;
import com.bradyrussell.uiscoin.transaction.Transaction;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Node {
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    Channel serverChannel;

    EventLoopGroup peerGroup = new NioEventLoopGroup();
    Bootstrap peerBootstrap = new Bootstrap();

    public ChannelGroup nodeClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); // nodes connections to me
    public ChannelGroup peerClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); // my connection to other nodes

    int Version;

    public Node(int Version) {
        this.Version = Version;
    }

    public void ConnectToPeer(InetAddress Address){
        if(getPeers().contains(Address)) {
            System.out.println("Already connected to this peer!");
            return;
        }

        peerBootstrap.group(peerGroup)
                .channel(NioSocketChannel.class)
                .handler(new NodeP2PClientInitializer(this));

        // Make a new connection.
        ChannelFuture sync = null;
        try {
            sync = peerBootstrap.connect(Address, MagicNumbers.NodeP2PPort.Value).sync();
            peerClients.add(sync.channel());

           // ChannelFuture closeFuture = sync.channel().closeFuture();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void RequestBlockFromPeers(BlockRequest request){
        peerClients.writeAndFlush(request);
        nodeClients.writeAndFlush(request);
    }

    public void BroadcastBlockToPeers(Block block){
        peerClients.writeAndFlush(block);
        nodeClients.writeAndFlush(block);
    }

    public void BroadcastTransactionToPeers(Transaction transaction){
        peerClients.writeAndFlush(transaction);
        nodeClients.writeAndFlush(transaction);
    }

    public void BroadcastPeerToPeers(InetAddress address){
        peerClients.writeAndFlush(address);
        nodeClients.writeAndFlush(address);
    }

    public void Start(){
        //startup the node server
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new NodeP2PServerInitializer(this));

        try {
            serverChannel = b.bind(MagicNumbers.NodeP2PPort.Value).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void Stop(){
        try {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
            peerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<InetAddress> getPeers(){
        List<InetAddress> addresses = new ArrayList<>();

        nodeClients.forEach((channel -> {
            InetAddress address = ((InetSocketAddress) channel.remoteAddress()).getAddress();
            if(!addresses.contains(address)) {
                addresses.add(address);
            }
        }));
        peerClients.forEach((channel -> {
            InetAddress address = ((InetSocketAddress) channel.remoteAddress()).getAddress();
            if(!addresses.contains(address)) {
                addresses.add(address);
            }
        }));

        return addresses;
    }
}
