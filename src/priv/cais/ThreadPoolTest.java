package priv.cais;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 如何合理地估算线程池大小 如果是CPU密集型应用，则线程池大小设置为N+1 如果是IO密集型应用，则线程池大小设置为2N+1
 */
public class ThreadPoolTest {
    private static final int NTHREADS = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
    // 使用 CachedThreadPool 比较耗内存，并发 200+的时候 会造成内存溢出
    // private static final ExecutorService exec = Executors.newCachedThreadPool();

    static long tempcount = System.currentTimeMillis() / 1000;// 用于计算每秒时间差
    static volatile int prenum = 100;// 用于计算任务数量差
    static int nums = 100;// 总任务数
    static int uses = 0;// 耗时 计数

    static long allstart = System.currentTimeMillis();// 程序启动时间 用于计算总耗时
    static long logstime = 0;// 日志总耗时

    public static void main(String[] args) {
        // QPS（TPS）：每秒钟request/事务 数量 ，QPS（TPS）= 并发数/平均响应时间
        // 并发数： 系统同时处理的request/事务数
        // 响应时间： 一般取平均响应时间

        for (int i = 0; i < nums; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        long start = System.currentTimeMillis();
                        task();
                        long use = System.currentTimeMillis() - start;
                        System.out.println("单个耗时" + use);
                        // getCurrentThreads(use);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                /**
                 * 任务目标
                 */
                public void task() {
                    FileInputStream fis = null;
                    BufferedInputStream bis = null;
                    FileOutputStream fos = null;
                    BufferedOutputStream bos = null;
                    try {
                        fis = new FileInputStream("D:\\123.pdf");
                        bis = new BufferedInputStream(fis);
                        fos = new FileOutputStream("D:\\456.pdf");
                        bos = new BufferedOutputStream(fos);
                        byte[] b = new byte[4096];
                        while (fis.read(b) > 0) {
                            bos.write(b);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (bis != null) {
                                bis.close();
                            }
                            if (fis != null) {
                                fis.close();
                            }
                            if (bos != null) {
                                bos.close();
                            }
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            };
            exec.execute(task);
        }
        exec.shutdown();
    }

    /**
     * 每个任务分析
     *
     * @param use
     *            单个任务耗时
     */
    public static void getCurrentThreads(long use) {
        // Map<Thread, StackTraceElement[]> maps = Thread.getAllStackTraces();
        // System.out.println("Threads:" + maps.size() + "-currentThread:" +
        // Thread.currentThread().getName() + "-"
        // + Thread.currentThread().getId());

        long start = System.currentTimeMillis();

        prenum = prenum - 1;// 任务数量 进度扣减
        uses += use;

        long now = System.currentTimeMillis() / 1000;
        if (now > tempcount) {// 每秒输出一次
            long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;// 已使用内存
            long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;// 总共可使用内存
            int cpu = Runtime.getRuntime().availableProcessors();// 可用cpu逻辑处理器

            System.out.printf("第%s秒: ", now - allstart / 1000);
            int ts = (nums - prenum);// 每秒事务数
            System.out.printf("每秒处理数:%s ", ts);
            System.out.printf("平均耗时:%s ", ts == 0 ? 0 : uses / ts);
            System.out.printf("进度:%s ", nums);
            System.out.printf("剩余:%s毫秒 ", nums * ts);
            System.out.printf("可用内存:%sm ", freeMemory);
            System.out.printf("总共可使用内存:%sm ", totalMemory);
            System.out.printf("可用CPU:%sm \n", cpu);
            tempcount = now;
            nums = prenum;
            uses = 0;
        }

        logstime += System.currentTimeMillis() - start;// 日志耗时累计

        // 当任务执行完了以后 计算总耗时
        if (prenum == 0) {
            long alluse = System.currentTimeMillis() - allstart;
            System.out.printf("总耗时%s毫秒,其中日志耗时%s毫秒\n", alluse, logstime);
            System.exit(0);
        }

    }

}