import io.vertx.core.buffer.Buffer;

/**
 * Created by sliu11 on 13/03/2017.
 */
public class BufferDemo {

    public static void main(String[] args) {
        Buffer buffer = Buffer.buffer(128);
        buffer.setUnsignedByte(15, (short)321);
        System.out.println(buffer.getUnsignedByte(15));
        System.out.println(buffer.getUnsignedByte(16));
    }
}
