package joshua.util.threadCpuTime;


import joshua.util.threadCpuTime.MyTask;
import joshua.util.threadCpuTime.ThreadCpuTimeKeeper;

//Source: http://www.javarticles.com/2016/02/java-thread-determining-cpu-time.html
public class ThreadCpuTimeExample {

 public static void main(String[] args) {

ThreadCpuTimeKeeper threadCpuTimeKeeper = ThreadCpuTimeKeeper.createThreadCpuTimeKeeper();

Thread[] threads = new Thread[3];
threads[0] = new Thread(new MyTask("Task1", 50000, threadCpuTimeKeeper), "Thread1");
threads[1] = new Thread(new MyTask("Task2", 60000, threadCpuTimeKeeper), "Thread2");
threads[2] = new Thread(new MyTask("Task3", 90000, threadCpuTimeKeeper), "Thread3");

threads[0].setDaemon(true);
threads[1].setDaemon(true);
threads[2].setDaemon(true);
threads[0].start();
threads[1].start();
threads[2].start();
// sleep so that other threads catch up
try {
   Thread.sleep(100);
} catch (InterruptedException e1) {
   e1.printStackTrace();
}

for (Thread thread : threads) {
   try {
 thread.join();
   } catch (InterruptedException e) {
 e.printStackTrace();
   }
}

// see:
// http://stackoverflow.com/questions/9063153/is-there-any-way-to-distinguish-the-main-thread-from-any-threads-that-it-spawns

// System.out.println("Timing report: "
// +
// threadCpuTimeKeeper.registerEndTimeMainThreadAndReportThreadTimeUsage());
System.out.println("Writing timing report...");
threadCpuTimeKeeper
 .registerEndTimeMainThreadAndReportThreadTimeUsageToFile("./CPUTimingReport.txt");

System.out.println("All threads finished their tasks");
 }
}