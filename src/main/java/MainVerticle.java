import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * Created by sliu11 on 13/03/2017.
 */
public class MainVerticle extends AbstractVerticle {


    @Override
    public void start(final Future<Void> startFuture) throws Exception {
        vertx.deployVerticle(HelloworldVerticle.class.getName(), res -> {
            if (res.succeeded()) {
                System.out.println(String.format("Deployment success"));
                startFuture.complete();
            } else {
                System.out.println(String.format("Deployment failed"));
                startFuture.fail(res.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        if (deploymentID() != null) {
            vertx.undeploy(deploymentID(), res -> {
                if (res.succeeded()) {
                    System.out.println(String.format("Deployment ID '%s' has been undeployed"));
                    stopFuture.complete();
                } else {
                    System.out.println(String.format("Deployment ID '%s' undeployment failed"));
                    stopFuture.fail(res.cause());
                }
            });
        }

    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(MainVerticle.class.getName(), res -> {
            if (res.succeeded()) {
                System.out.println("MainVerticle deployed");
            } else {
                System.out.println("MainVerticle deploy failed");
            }
        });

        final int[] number = {0};
        vertx.setPeriodic(2000, id -> {
            if (number[0] == 5) {
                vertx.cancelTimer(id);
                return;
            }
            System.out.println("Hello " + id);
            number[0]++;
        });
    }
}
