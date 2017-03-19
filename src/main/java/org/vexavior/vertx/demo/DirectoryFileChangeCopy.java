package org.vexavior.vertx.demo;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import javafx.util.Pair;

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
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by sliu11 on 17/03/2017.
 */
public class DirectoryFileChangeCopy {

    private static class CopyPathsResolver {

        private Path scannedDir;
        private Path rootSrcPath;
        private Path rootDstPath;

        private CopyPathsResolver(Path scannedDir, Path rootSrcPath, Path rootDstPath) {
            this.scannedDir = scannedDir;
            this.rootSrcPath = rootSrcPath;
            this.rootDstPath = rootDstPath;
        }

        Path resolveSrcPath(Path filePath) {
            return scannedDir.resolve(filePath);
        }

        Path resolveDstPath(Path filePath) {
            Path srcPath = resolveSrcPath(filePath);
            Path relPath = rootSrcPath.relativize(srcPath);
            return rootDstPath.resolve(relPath);
        }

        String pathToString(Path path) {
            return path.toString();
        }
    }

    private static class FileCopyExecutor {
        private Vertx vertx;

        FileCopyExecutor(Vertx vertx) {
            this.vertx = vertx;
        }

        Future<Void> copy(Path srcPath, Path dstPath, CopyPathsResolver resolver) {
            String dstPathParentStr = resolver.pathToString(dstPath.getParent());
            String dstPathStr = resolver.pathToString(dstPath);
            String srcPathStr = resolver.pathToString(srcPath);

            FileSystem fs = vertx.fileSystem();
            Future<Void> startFuture = Future.future();
            Future<Boolean> f1 = Future.future();
            fs.exists(dstPathParentStr, f1.completer());

            f1.compose(v -> {
                Future<Void> f2 = Future.future();
                if (!v) {
                    fs.mkdirs(dstPathParentStr, f2.completer());
                } else {
                    f2.complete();
                }
                return f2;
            }).compose(v -> {
                Future<Boolean> f3 = Future.future();
                fs.exists(dstPathStr, f3.completer());
                return f3;
            }).compose(v -> {
                Future<Void> f4 = Future.future();
                if (v) {
                    fs.delete(dstPathStr, f4.completer());
                } else {
                    f4.complete();
                }
                return f4;
            }).compose(v -> {
                fs.copy(srcPathStr, dstPathStr, startFuture.completer());
            }, startFuture);
            return startFuture;
        }
    }


    public static final String FILENAME_TO_WATCH = "^(?:[^.]+)+(?:\\.jsp|\\.css|\\.js)$";
    private static HashMap<WatchKey, Pair<Path, Path>> keys = new HashMap<>();

    private static Map<WatchKey, CopyPathsResolver> resolverMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();

        String[] srcRootUriStrs = new String[]{
                "file:///C:/Users/sliu11/MacLeasing/Projects/iPortal/iportal/src/site",
                "file:///C:/Users/sliu11/MacLeasing/Projects/iPortal/iportal/src/main/webapp"
        };

        URI destUri = URI.create("file:///C:/Users/sliu11/MacLeasing/Projects/iPortal/iportal/target/iportal");
        Path rootDstPath = Paths.get(destUri);

        WatchService watchService = FileSystems.getDefault().newWatchService();

        for (String srcRootUriStr : srcRootUriStrs) {
            URI srcRootUri = URI.create(srcRootUriStr);
            Path rootSrcPath = Paths.get(srcRootUri);
            CopyPathsResolver resolver = new CopyPathsResolver(rootSrcPath, rootSrcPath, rootDstPath);

            WatchKey k = rootSrcPath.register(watchService, ENTRY_MODIFY);
            keys.put(k, new Pair<>(rootSrcPath, rootSrcPath));
            resolverMap.put(k, resolver);

            Files.walkFileTree(rootSrcPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    WatchKey k = dir.register(watchService, ENTRY_MODIFY);
                    keys.put(k, new Pair<>(dir, rootSrcPath));
                    resolverMap.put(k, new CopyPathsResolver(dir, rootSrcPath, rootDstPath));
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
                    Path filePath = (Path) event.context();

                    CopyPathsResolver resolver = resolverMap.get(watchKey);
                    if (resolver == null) continue;
                    if (!resolver.pathToString(filePath).matches(FILENAME_TO_WATCH)) continue;
                    Path srcPath = resolver.resolveSrcPath(filePath);
                    Path dstPath = resolver.resolveDstPath(filePath);
                    if (srcPath.toFile().isDirectory()) continue;

                    Future<Void> startFuture = new FileCopyExecutor(vertx).copy(srcPath, dstPath, resolver);
                    startFuture.setHandler(res -> {
                        String srcPathStr = resolver.pathToString(srcPath);
                        String dstPathStr = resolver.pathToString(dstPath);
                        if (res.succeeded()) {
                            System.out.println(String.format("Copy from '%s' to '%s' success", srcPathStr, dstPathStr));
                        } else {
                            System.err.println(String.format("Copy from '%s' to '%s' failed", srcPathStr, dstPathStr));
                            startFuture.cause().printStackTrace(System.err);
                        }
                    });
                }
                if (!watchKey.reset()) {
                    keys.remove(watchKey);
                    resolverMap.remove(watchKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
