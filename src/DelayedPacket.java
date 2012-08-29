
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.DatagramSocket;

/**
 * Created by IntelliJ IDEA.
 * User: Thomas
 * Date: 05.07.2004
 * Time: 19:10:08
 * To change this template use File | Settings | File Templates.
 */
public class DelayedPacket {
    public DatagramPacket packet;
    public long time;
    public DatagramSocket ds;

    DelayedPacket(DatagramPacket packet, DatagramSocket ds, long time) throws SocketException {
        this.packet = new DatagramPacket((byte[]) packet.getData().clone(),packet.getLength(),packet.getSocketAddress());
        this.time = time;
        this.ds = ds;
    }

}
