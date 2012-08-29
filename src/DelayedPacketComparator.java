
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: Thomas
 * Date: 05.07.2004
 * Time: 20:30:22
 * To change this template use File | Settings | File Templates.
 */
public class DelayedPacketComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        return (int) (((DelayedPacket)o1).time - ((DelayedPacket)o2).time);
    }
}
