import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

/**
 * Created by sliu11 on 13/03/2017.
 */
public class EventBusDemo {

    public static final String NEWS_UK_SPORTS = "news.uk.sports";

    public static void main(String[] args) {
        EventBus eventBus = Vertx.vertx().eventBus();

        eventBus.<String>consumer(NEWS_UK_SPORTS, message -> {
            System.out.println(
                    String.format("[%s@Handler1]: I have received a message '%s'",
                            Thread.currentThread().getName(), message.body()));
            message.reply("Okokokokk");
        });

        eventBus.<String>consumer(NEWS_UK_SPORTS, message -> {
            System.out.println(
                    String.format("[%s@Handler2]: I have received a message '%s'",
                            Thread.currentThread().getName(), message.body()));

            message.reply("Noooonononononono");
        });


        eventBus.<String>consumer(NEWS_UK_SPORTS, message -> {
            System.out.println(
                    String.format("[%s@Handler3]: I have received a message '%s'",
                            Thread.currentThread().getName(), message.body()));

            message.reply("Klsdjwiq9cndskjdfi");
        });


        eventBus.publish(NEWS_UK_SPORTS, "Yaeks! G'day! Blablablabla");
        for(int i = 0; i < 6; i++) {
            eventBus.<String>send(NEWS_UK_SPORTS, "Hahahaha! This is only for one of you guys!", ar -> {
                System.out.println("Reply received:" + ar.result().body());
            });
        }
    }
}
