package org.vexavior.vertx.demo;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by sliu11 on 17/03/2017.
 */
public class DirectoryFileChangeCopy {
    public static final String FILENAME_TO_WATCH = "^(?:[^.]+)+(?:\\.jsp|\\.css|\\.js)$";
    private static HashMap<WatchKey, Pair<Path, Path>> keys = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();

        String[] srcRootUriStrs = new String[]{
                "file:///C:/Users/sliu11/MacLeasing/Projects/iPortal/iportal/src/site",
                "file:///C:/Users/sliu11/MacLeasing/Projects/iPortal/iportal/src/main/webapp"
        };

        URI destUri = URI.create("file:///C:/Users/sliu11/MacLeasing/Projects/iPortal/iportal/target/iportal");
        Path destPath = Paths.get(destUri);

        WatchService watchService = FileSystems.getDefault().newWatchService();

        for (String srcRootUriStr : srcRootUriStrs) {
            URI srcRootUri = URI.create(srcRootUriStr);
            Path srcRootPath = Paths.get(srcRootUri);
            WatchKey k = srcRootPath.register(watchService, ENTRY_MODIFY);
            keys.put(k, new Pair<>(srcRootPath, srcRootPath));

            Files.walkFileTree(srcRootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    WatchKey k = dir.register(watchService, ENTRY_MODIFY);
                    keys.put(k, new Pair<>(dir, srcRootPath));
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        vertx.setPeriodic(1000L, id -> {
            try {
                WatchKey watchKey = watchService.poll();
                if (watchKey == null) return;
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind != ENTRY_MODIFY) continue;
                    Path path = (Path) event.context();
                    Pair<Path, Path> dirPair = keys.get(watchKey);
                    if (dirPair == null) continue;
                    Path dir = dirPair.getKey();
                    Path srcRootPath = dirPair.getValue();
                    Path srcChild = dir.resolve(path);
                    File childFile = srcChild.toFile();
                    if (childFile.isDirectory()) continue;
                    if (!childFile.getName().matches(FILENAME_TO_WATCH)) continue;
                    Path rchild = srcRootPath.relativize(srcChild);
                    Path destChild = destPath.resolve(rchild);
                    String srcChildStr = srcChild.toFile().getAbsolutePath();
                    String destChildStr = destChild.toFile().getAbsolutePath();
                    String destPathParentStr = destChild.getParent().toFile().getAbsolutePath();

                    FileSystem fs = vertx.fileSystem();
                    Future<Void> startFuture = Future.future();
                    Future<Boolean> f1 = Future.future();
                    fs.exists(destPathParentStr, f1.completer());

                    f1.compose(v -> {
                        Future<Void> f2 = Future.future();
                        if (!v) {
                            fs.mkdirs(destChild.getParent().toString(), f2.completer());
                        } else {
                            f2.complete();
                        }
                        return f2;
                    }).compose(v -> {
                        Future<Boolean> f3 = Future.future();
                        fs.exists(destChildStr, f3.completer());
                        return f3;
                    }).compose(v -> {
                        Future<Void> f4 = Future.future();
                        if (v) {
                            fs.delete(destChildStr, f4.completer());
                        } else {
                            f4.complete();
                        }
                        return f4;
                    }).compose(v -> {
                        fs.copy(srcChildStr, destChildStr, startFuture.completer());
                    }, startFuture);

                    startFuture.setHandler(res -> {
                        if (res.succeeded()) {
                            System.out.println(String.format("Copy from '%s' to '%s' success", srcChildStr, destChildStr));
                        } else {
                            System.err.println(String.format("Copy from '%s' to '%s' failed", srcChildStr, destChildStr));
                            System.err.println(startFuture);
                        }
                    });
                }
                if (!watchKey.reset()) keys.remove(watchKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
