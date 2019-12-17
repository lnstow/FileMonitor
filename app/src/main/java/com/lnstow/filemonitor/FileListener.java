package com.lnstow.filemonitor;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

public class FileListener {

    //  /proc/sys/fs/inotify/max_user_instances 初始化 ifd 的数量限制
    //  /proc/sys/fs/inotify/max_queued_events ifd 文件队列长度限制
    //  /proc/sys/fs/inotify/max_user_watches 注册监听目录的数量限制
    //  既然是文件描述符，当然也受 /etc/security/limits.conf 和 /proc/sys/fs/file-max 限制
    //  https://www.jianshu.com/p/46b2bfad3d61

    static Map<String, SingleFileListener> dirMap = new ConcurrentHashMap<>(1024 * 8);
    static String[] FullInfo;
    static Handler handler;
    static String[] excludeDir;
    static int excludeLen;

    static int mMask;
    static int number;
    static boolean flagListen;
    static int fileLen;
    File mFile;
    ForkJoinPool forkJoinPool;
    private Handler tempHandler;
    static ExecutorService singleThread;
    static boolean flagSend;
    static boolean flagFork;

    public FileListener(String path, int mask, Handler handler, String[] excludeDir) {
//        mFile = Environment.getExternalStorageDirectory();
        mFile = new File(path);
        FileListener.mMask = mask;
        tempHandler = handler;
        FileListener.number = 0;
        FileListener.flagListen = false;
        FileListener.fileLen = path.length();
        FileListener.flagSend = true;
        FileListener.excludeDir = excludeDir;
        FileListener.excludeLen = excludeDir.length;
        FileListener.flagFork = true;
    }

    public void startListen() {
//        String parallelism = String.valueOf(4 + 2 * Runtime.getRuntime().availableProcessors());
//        System.out.println(parallelism);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        forkJoinPool = ForkJoinPool.commonPool();
        singleThread = Executors.newSingleThreadExecutor();
//        if (dirMap.size() != 0) {
//            singleThread.execute(() -> {
//                Iterator<Map.Entry<String, SingleFileListener>> iterator = dirMap.entrySet().iterator();
//                while (iterator.hasNext()) {
//                    iterator.next().getValue().stopWatching();
//                    iterator.remove();
//                }
//            });
//        }
        handler = tempHandler;
        singleThread.execute(() -> {
            long t1 = System.currentTimeMillis();

            forkJoinPool.execute(new WalkFileTree(mFile));
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            forkJoinPool.awaitQuiescence(3000, TimeUnit.MILLISECONDS);
            FileListener.FullInfo = new String[1024 * 1024];
            FileListener.flagListen = true;


            Message message = Message.obtain();
            message.what = 16;
            message.obj = new StringBuilder().append(dirMap.size()).append("dir ")
                    .append(System.currentTimeMillis() - t1).append("ms");
            FileListener.handler.sendMessage(message);

//                try {
//                    TimeUnit.MILLISECONDS.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                message = Message.obtain();
//                message.what = 16;
//                message.obj = dirMap.size() + "  " + (System.currentTimeMillis() - t1);
//                FileListener.handler.sendMessage(message);
        });
        final WeakReference<FileListener> weakReference = new WeakReference<>(this);
        singleThread.execute(() -> {
//            System.out.println("map  "+dirMap.size());
            if (dirMap.size() > 8192) {
                FileListener.flagFork = false;
                Message message = Message.obtain();
                message.what = 10;
                message.obj = "监视目录超过8192个，请排除部分目录以减少数量";
                FileListener.handler.sendMessage(message);
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                weakReference.get().stopListen();
            }
        });

    }

    public void stopListen() {
//        singleThread.execute(() -> {
        Iterator<Map.Entry<String, SingleFileListener>> iterator = dirMap.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().getValue().stopWatching();
            iterator.remove();
        }
//        });
//        dirMap.clear();
//        if (handler != null)
//            handler.removeCallbacksAndMessages(null);
        handler = null;
        FullInfo = null;
        if (singleThread != null)
            singleThread.shutdown();
    }

//    @RequiresApi(26)
//    public void start2() {
//        try {
//            Path parent = Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath());
//            Files.walkFileTree(parent, new aaa());
//        } catch (
//                IOException e) {
//            e.printStackTrace();
//        }
//    }


}

class SingleFileListener extends FileObserver {

    String mPath;
    private static String operation;
    private static String oldName;
    private static String newName;
    private static final String TAG = "123123";
    private static final int DIR = 0x40000000;
    private static long t1 = 0;
    private static long t2 = 0;
    private static int c1 = 0;
//    private static AtomicInteger ts=new AtomicInteger(0);

    SingleFileListener(String mPath, int mask) {
        super(mPath, mask);
        this.mPath = mPath;
    }

    @Override
    public void onEvent(int event, @Nullable String path) {
//        Log.d(TAG, event + "  " + path);
        if (!FileListener.flagListen || path == null) {
            return;
        }
//        int len = path.length();
//        if (path.charAt(--len) == 'l' && path.charAt(--len) == 'l' && path.charAt(--len) == 'u' && path.charAt(--len) == 'n') {
//            return;
//        }
        path = mPath + "/" + path;
        boolean isDir = (event & DIR) != 0;
        switch (event & FileObserver.ALL_EVENTS) {
            case FileObserver.ACCESS:
                if (isDir) {
                    operation = "Dir ACCESS";
                } else {
                    operation = "File ACCESS";
                }
                break;

            case FileObserver.MODIFY:
                if (isDir) {
                    operation = "Dir MODIFY";
                } else {
                    operation = "File MODIFY";
                }
                break;

            case FileObserver.CLOSE_WRITE:
            case FileObserver.CLOSE_NOWRITE:
                if (isDir) {
                    operation = "Dir CLOSE";
                } else {
                    operation = "File CLOSE";
                }
                break;

            case FileObserver.OPEN:
                if (isDir) {
                    operation = "Dir OPEN";
                } else {
                    operation = "File OPEN";
                }
                break;

            case FileObserver.CREATE:
                if (isDir) {
                    SingleFileListener singleFileListener = new SingleFileListener(path, FileListener.mMask);
                    singleFileListener.startWatching();
                    FileListener.dirMap.put(path, singleFileListener);
                    operation = "Dir CREATE";
                } else {
                    operation = "File CREATE";
                }
                break;

            case FileObserver.DELETE:
            case FileObserver.DELETE_SELF:
                if (isDir) {
                    if (FileListener.dirMap.containsKey(path)) {
                        FileListener.dirMap.get(path).stopWatching();
                        FileListener.dirMap.remove(path);
                    }
                    operation = "Dir DELETE";
                } else {
                    operation = "File DELETE";
                }
                break;

            case FileObserver.MOVED_FROM:
                if (isDir) {
                    oldName = path;
                    operation = "Dir MVFROM";
                } else {
                    operation = "File MVFROM";
                }
                break;
            case FileObserver.MOVED_TO:
                if (isDir) {
//                    if (oldName != null) {
                    newName = path;

                    FileListener.singleThread.execute(new Runnable() {
                        String oldNameTemp = oldName;
                        String newNameTemp = newName;

                        @Override
                        public void run() {
                            String newPath;
                            int len = oldNameTemp.length();
                            Iterator<Map.Entry<String, SingleFileListener>> iterator = FileListener.dirMap.entrySet().iterator();
                            Map.Entry<String, SingleFileListener> entry;
                            while (iterator.hasNext()) {
                                entry = iterator.next();
                                if (entry.getKey().startsWith(oldNameTemp)) {
                                    newPath = newNameTemp + entry.getKey().substring(len);
                                    entry.getValue().mPath = newPath;
                                    iterator.remove();
                                    FileListener.dirMap.put(newPath, entry.getValue());
                                }
                            }

                        }
                    });

//                        oldName = null;
//                    }
                    operation = "Dir MVTO";
                } else {
                    operation = "File MVTO";
                }
                break;
            case FileObserver.MOVE_SELF:
                if (isDir) {
                    operation = "Dir MVSELF";
                } else {
                    operation = "File MVSELF";
                }
                break;

            case FileObserver.ATTRIB:
                if (isDir) {
                    operation = "Dir ATTRIB";
                } else {
                    operation = "File ATTRIB";
                }
                break;

            default:
                return;
        }

        FileListener.FullInfo[FileListener.number] = String.format(Locale.US, "%-6d%-10tT%-13s%s",
                ++FileListener.number, Calendar.getInstance(), operation, path.substring(FileListener.fileLen));

        t2 = System.currentTimeMillis();
        if (t2 - t1 > 50 || FileListener.number - c1 > 500) {
            if (FileListener.flagSend) {
                Message message = Message.obtain();
                message.what = 18;
                message.arg1 = c1;
                message.arg2 = FileListener.number;
                FileListener.handler.sendMessage(message);
            }
            t1 = t2;
            c1 = FileListener.number;
        }
//        System.out.println("testint  "+ts.addAndGet(1));

    }
}

class WalkFileTree extends RecursiveAction {
    File file;

    WalkFileTree(File file) {
        this.file = file;
    }

    @Override
    protected void compute() {
        String path = file.getAbsolutePath();
        for (int j = 0; j < FileListener.excludeLen; j++) {
            if (path.equals(FileListener.excludeDir[j]))
                return;
        }
        File[] children = file.listFiles();
        if (children == null)
            return;

        int len = children.length;
        for (int i = 0; i < len; i++) {
            if (FileListener.flagFork && children[i].canRead() && children[i].isDirectory()) {
                new WalkFileTree(children[i]).fork();
            }
        }
        SingleFileListener singleFileListener = new SingleFileListener(path, FileListener.mMask);
        singleFileListener.startWatching();
        FileListener.dirMap.put(path, singleFileListener);
    }
}

//@RequiresApi(26)
//class aaa extends SimpleFileVisitor<Path> {
//    public static int count = 0;
//
//    @Override
//    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//        count++;
//        return super.postVisitDirectory(dir, exc);
//    }
//
//    @Override
//    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//        exc.printStackTrace();
//        return FileVisitResult.CONTINUE;
//    }
//
//    @Override
//    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//        count++;
//        return super.visitFile(file, attrs);
//    }
//}


