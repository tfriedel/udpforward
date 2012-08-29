
import EDU.oswego.cs.dl.util.concurrent.BoundedPriorityQueue;

import java.net.*;
import java.io.IOException;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: Thomas
 * Date: 05.07.2004
 * Time: 17:11:40
 * To change this template use File | Settings | File Templates.
 */
public class UDPforward extends Thread {
    private Random rnd;
    private InetAddress b;
    private int destport;
    private int port;
    private DatagramSocket fromA;
    private DatagramSocket toB;
    private SocketAddress a;
    private double packetloss;
    private int minDelay;
    private int maxDelay;
    private BoundedPriorityQueue pq;
    private Thread sched;
    boolean debug = false;

    class Scheduler implements Runnable {
        public void run() {
            DelayedPacket dp;
            long waitTime = 0;
            while (true) {
                synchronized (pq) {
                    if ((dp = (DelayedPacket) pq.peek()) != null) {
                        waitTime = dp.time - System.currentTimeMillis();
                        try {
                            if (waitTime > 0)
                                pq.wait(waitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if ((dp = (DelayedPacket) pq.peek()).time <= System.currentTimeMillis()) {
                            try {
                                pq.take();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            try {
                                dp.ds.send(dp.packet);
                                if (debug) {
                                    System.out.println("Sending packet of length "+ dp.packet.getLength() +
                                            " scheduled at " + dp.time + " (current time: " + System.currentTimeMillis() + " )" +
                                            " from " + dp.ds.getLocalPort() + " to " + dp.packet.getPort());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else
                        try {
                            pq.wait(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                }
            }
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        UDPforward AtoB = new UDPforward();
        AtoB.fromAtoB(args);
    }

    public synchronized SocketAddress getA() {
        return a;
    }

    public synchronized void setA(SocketAddress a) {
        this.a = a;
    }

    void fromAtoB(String[] args) throws IOException, InterruptedException {
        rnd = new Random();
        //debug = false;
        b = null;
        destport = 0;
        port = Integer.parseInt(args[0]);
        pq = new BoundedPriorityQueue(1024, new DelayedPacketComparator());
        sched = new Thread(new Scheduler());
        sched.start();

        try {
            if ((args.length) < 2) {
                System.out.println("Syntax: UDPforward localport host:port [packetloss [minDelay [maxDelay]]]");
                System.out.println("Everything you send to localport is forwarded to host:port.");
                System.out.println("Every response we get on the port used to talk to host:port is sent back to the");
                System.out.println("the originator of the last message which was received on localport.");
                System.out.println("packetloss is a ratio between 0 and 1. A ratio of 0.l means, we loose 10% percent of the packets.");
                System.out.println("minDelay and maxDelay specify in ms the time a packet can be delayed. If you omit maxDelay,");
                System.out.println("it is assumed minDelay=maxDelay.");
            }
            // parse destination address
            String host;
            String[] d = args[1].split(":");
            host = d[0];
            if (d.length != 2) {
                System.out.println("problem parsing destionation address");
                System.exit(-3);
            }
            destport = Integer.parseInt(d[1]);
            b = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host.");
            System.exit(-2);
        }
        packetloss = 0;
        minDelay = 0;
        maxDelay = 0;
        if ((args.length) > 2)
            packetloss = Double.parseDouble(args[2]);
        if ((args.length) > 3) {
            minDelay = Integer.parseInt(args[3]);
            maxDelay = minDelay;
        }
        if ((args.length) > 4)
            maxDelay = Integer.parseInt(args[4]);

        fromA = new DatagramSocket(port);
        toB = new DatagramSocket();
        byte[] buf = new byte[8192];
        DatagramPacket p;
        p = new DatagramPacket(buf, buf.length, b, destport);
        fromA.receive(p);
        setA(p.getSocketAddress());
        this.start(); // spawn thread
        while (true) {
            p.setPort(destport);
            p.setAddress(b);
            newSend(p, toB);
            p.setLength(8192);
            fromA.receive(p);
            setA(p.getSocketAddress());
        }
    }

    private void newSend(DatagramPacket p, DatagramSocket where) throws InterruptedException, SocketException {
        if ((packetloss == 0) || (Math.random() > packetloss)) {
            long when = System.currentTimeMillis() + minDelay + rnd.nextInt(maxDelay + 1 - minDelay);
            synchronized (pq) {
                pq.put(new DelayedPacket(p, where, when));
                pq.notifyAll();
            }
        } else if(debug)
            System.out.println("discarding packet");

    }

    public void run() {
        byte[] buf = new byte[8192];
        DatagramPacket p;
        p = new DatagramPacket(buf, buf.length, b, destport);

        while (true) {
            try {
                p.setLength(8192);
                toB.receive(p);
                p.setSocketAddress(getA());
                try {
                    newSend(p, fromA);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
