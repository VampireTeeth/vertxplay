import io.vertx.core.AbstractVerticle;

/**
 * Created by sliu11 on 13/03/2017.
 */
public class HelloworldVerticle extends AbstractVerticle {


    @Override
    public void start() throws Exception {
        System.out.println("Hello world start");
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Hello world stop");
    }

}
