package org.vexavior.vertx.demo;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    private static class Config {
        private final DocumentContext jsonDoc;
        private final List<String> srcRootDirs;
        private final String dstRootDir;
        private final String filenameRegex;

        List<String> getSrcRootDirs() {
            return srcRootDirs;
        }

        String getDstRootDir() {
            return dstRootDir;
        }

        String getFilenameRegex() {
            return filenameRegex;
        }

        private Config() throws Exception {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("directory-scan-copy.json");
            try {
                jsonDoc = JsonPath.parse(is);
            } finally {
                is.close();
            }
            srcRootDirs = parseSrcRootDirectories();
            dstRootDir = parseDstRootDirectory();
            filenameRegex = parseFilenameRegex();
        }

        private List<String> parseSrcRootDirectories() throws Exception {
            String[] res = jsonDoc.read("$.srcRootDirs", String[].class);
            checkNull(res, "srcRootDirs not found");
            List<String> ret = new ArrayList<>();
            Arrays.asList(res).forEach(val -> ret.add(mkUri(val)));
            return ret;
        }

        private String parseDstRootDirectory() throws Exception {
            String res = jsonDoc.read("$.dstRootDir", String.class);
            checkNull(res, "dstRootDir not found");
            return mkUri(res);
        }

        private String parseFilenameRegex() throws Exception {
            String res = jsonDoc.read("$.filenameRegex", String.class);
            checkNull(res, "filenameMatchRegex not found");
            return res;
        }

        private String mkUri(String obj) {
            String val = obj;
            val = val.matches("^/") ? val : String.format("/%s", val);
            val = String.format("file://%s", val);
            return val;
        }

        private void checkNull(Object v, String msg) throws Exception {
            if (v == null) {
                throw new Exception(msg);
            }
        }

    }

    private static HashMap<WatchKey, Pair<Path, Path>> keys = new HashMap<>();

    private static Map<WatchKey, CopyPathsResolver> resolverMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();
        Config config = new Config();

        List<String> srcRootUriStrs = config.getSrcRootDirs();

        URI destUri = URI.create(config.getDstRootDir());
        String filenameRegex = config.getFilenameRegex();
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
                    if (!resolver.pathToString(filePath).matches(filenameRegex)) continue;
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
