package joshua.util.threadCpuTime;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to maintain a bookkeeping of CPU time usage. Threads/Runnables that use this
 * class should include the ThreadCpuTimer in their class, and call the interface methods
 * "startThreadCpuTimer" once the computation begins and "endThreadCpuTimer" once the runnable
 * stops.
 * 
 * The CPU time keeping is complicated by the fact that valid values can only be obtained as long as
 * a Thread is still running, hence a callback mechanism is needed, see e.g.
 * http://stackoverflow.com/questions/17255498/implementing-callbacks-in-java- with-runnable
 * 
 * The main thread (thread of the main method) is a special case. When ThreadCpuTimeKeeper is
 * created, it is assumed that this is done from the main thread, and the start time for the main
 * thread is immediately registered.
 * 
 * The main functionality comes from the method "registerEndTimeMainThreadAndReportThreadTimeUsage"
 * which first registers the end time for the main thread and then produces a report of the time
 * usage by the different threads and the total time used.
 * 
 * See also: http://nadeausoftware.com/articles/2008/03/
 * java_tip_how_get_cpu_and_user_time_benchmarking
 * 
 * (This gives a good overview of the issues with cpu time benchmarking).
 * 
 * @author gemaille
 *
 */
public class ThreadCpuTimeKeeper implements ThreadCpuTimer {
  private static String CSV_FILE_SEPARATOR = ",";
  private static String NL = "\n";
  private static int NUM_FRACTION_DIGITS = 4;
  private static DecimalFormat DECIMAL_FORMAT = createNumberFormatter();

  private static DecimalFormat createNumberFormatter() {
    NumberFormat nf = NumberFormat.getNumberInstance();
    DecimalFormat df = (DecimalFormat) nf;
    df.setMinimumFractionDigits(NUM_FRACTION_DIGITS);
    df.setMaximumFractionDigits(NUM_FRACTION_DIGITS);
    return df;
  }

  private static long NANOSECONDS_PER_SECOND = 1000000000;

  private final long mainThreadID;
  // Multiple threads may register their start and end times concurrently, so we use a concurrent
  // map to keep
  // track of those
  private final ConcurrentHashMap<Long, ThreadCpuTimeStampDifference> threadIDToCpuTimingMap;
  private final WallClockTimeDifference wallClockTimeDifference;

  private ThreadCpuTimeKeeper(long mainThreadID,
      ConcurrentHashMap<Long, ThreadCpuTimeStampDifference> threadIDToCpuTimingMap,
      WallClockTimeDifference wallClockTimeDifference) {
    this.mainThreadID = mainThreadID;
    this.threadIDToCpuTimingMap = threadIDToCpuTimingMap;
    this.wallClockTimeDifference = wallClockTimeDifference;
  }

  public static ThreadCpuTimeKeeper createThreadCpuTimeKeeper() {
    ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
    ConcurrentHashMap<Long, ThreadCpuTimeStampDifference> threadIDToCpuTimingMap = new ConcurrentHashMap<>();
    ThreadCpuTimeKeeper result = new ThreadCpuTimeKeeper(getMainTrheadID(), threadIDToCpuTimingMap,
        WallClockTimeDifference.createWallClockTimeDifference());
    result.registerThreadStartTime(Thread.currentThread());
    return result;
  }

  private static long getMainTrheadID() {
    return Thread.currentThread().getId();
  }

  private boolean isMainThread(long threadID) {
    return threadID == this.mainThreadID;
  }

  /**
   * Register a thread start time. We assume every thread has a unique ID 
   * and throw a runtime Exception if this is not the case.
   * @param thread
   */
  public void registerThreadStartTime(Thread thread) {
    long threadId = thread.getId();

    if (threadIDToCpuTimingMap.containsKey(threadId)) {
      throw new RuntimeException(
          "Error: start time was already registered for thread with ID: " + threadId);
    }

    ThreadCpuTimeStampDifference threadCpuTimeStampDifference = ThreadCpuTimeStampDifference
        .createThreadCpuTimeStampDifference(thread);
    threadIDToCpuTimingMap.putIfAbsent(threadId, threadCpuTimeStampDifference);
  }

  public void registerThreadEndTime(Thread thread) {
    long threadId = thread.getId();

    if (!threadIDToCpuTimingMap.containsKey(threadId)) {
      throw new RuntimeException(
          "Error: trying to register thread end time, but start time was not registerd for thread with ID: "
              + threadId);
    }

    ThreadCpuTimeStampDifference threadCpuTimeStampDifference = threadIDToCpuTimingMap
        .get(thread.getId());
    threadCpuTimeStampDifference.registerCpuEndTimeStamp(thread);
    wallClockTimeDifference.registerWallClockEndTime();

  }

  private void registerThreadEndTimeMainThread() {
    // Register the (current) end time for the main thread
    registerThreadEndTime(Thread.currentThread());

  }

  public static long getCpuTime(Thread thread) {
    ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
    if (mxBean.isThreadCpuTimeSupported()) {
      try {
        return mxBean.getThreadCpuTime(thread.getId());
      } catch (UnsupportedOperationException e) {
        System.out.println(e.toString());
      }
    } else {
      System.out.println("Not supported");
    }
    return 0;
  }

  public static long getUserTime(Thread thread) {
    ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
    if (mxBean.isThreadCpuTimeSupported()) {
      try {
        return mxBean.getThreadUserTime(thread.getId());

      } catch (UnsupportedOperationException e) {
        System.out.println(e.toString());
      }
    } else {
      System.out.println("Not supported");
    }
    return 0;
  }

  /**
   * This method registers the running start time for the provided thread. It should be called
   * inside the run method of the Runnable/Thread class for which timing is desired, at the start of
   * the run method.
   */
  @Override
  public void startThreadCpuTimer(Thread thread) {
    registerThreadStartTime(thread);
  }

  /**
   * This method registers the running end time for the provided thread. It should be called inside
   * the run method of the Runnable/Thread class for which timing is desired, at the end of the run
   * method (just before it returns)
   */
  @Override
  public void stopThreadCpuTimer(Thread thread) {
    registerThreadEndTime(thread);
  }

  public long getTotalSomethingTimeSpentByAllThreads(
      ThreadCpuTimeDifferenceGetTimeOperation threadCpuTimeDifferenceGetTimeOperation) {
    long result = 0;
    for (Entry<Long, ThreadCpuTimeStampDifference> entry : threadIDToCpuTimingMap.entrySet()) {
      result += threadCpuTimeDifferenceGetTimeOperation.getTime(entry.getValue());
    }
    return result;
  }

  public long getTotalSomethingTimeSpentByAllExceptMainThread(
      ThreadCpuTimeDifferenceGetTimeOperation threadCpuTimeDifferenceGetTimeOperation) {
    long result = 0;
    for (Entry<Long, ThreadCpuTimeStampDifference> entry : threadIDToCpuTimingMap.entrySet()) {
      if (!isMainThread(entry.getKey())) {
        result += threadCpuTimeDifferenceGetTimeOperation.getTime(entry.getValue());
      }
    }
    return result;
  }

  public long getTotalSomethingTimeSpentByMainThread(
      ThreadCpuTimeDifferenceGetTimeOperation threadCpuTimeDifferenceGetTimeOperation) {
    return threadCpuTimeDifferenceGetTimeOperation
        .getTime(this.threadIDToCpuTimingMap.get(getMainTrheadID()));

  }

  public long getTotalCpuTimeSpentByAllThreads() {
    return getTotalSomethingTimeSpentByAllThreads(getCpuTimeOperation);
  }

  public long getTotalCpuTimeSpentByMainThread() {
    return getTotalSomethingTimeSpentByMainThread(getCpuTimeOperation);
  }

  public long getTotalCpuTimeSpentByAllExceptMainThread() {
    return getTotalSomethingTimeSpentByAllExceptMainThread(getCpuTimeOperation);
  }

  public long getTotalUserTimeSpentByAllThreads() {
    return getTotalSomethingTimeSpentByAllThreads(getUserTimeOperation);
  }

  public long getTotalUserTimeSpentByMainThread() {
    return getTotalSomethingTimeSpentByMainThread(getUserTimeOperation);
  }

  public long getTotalUserTimeSpentByAllExceptMainThread() {
    return getTotalSomethingTimeSpentByAllExceptMainThread(getUserTimeOperation);
  }

  public long getTotalSystemTimeSpentByAllThreads() {
    return getTotalSomethingTimeSpentByAllThreads(getSystemTimeOperation);
  }

  public long getTotalSystemTimeSpentByMainThread() {
    return getTotalSomethingTimeSpentByMainThread(getSystemTimeOperation);
  }

  public long getTotalSystemTimeSpentByAllExceptMainThread() {
    return getTotalSomethingTimeSpentByAllExceptMainThread(getSystemTimeOperation);
  }

  // See:
  // http://stackoverflow.com/questions/924208/how-to-convert-nanoseconds-to-seconds-using-the-timeunit-enum
  private double convertNanoSecondsToSeconds(long nanoSeconds) {
    double seconds = (double) nanoSeconds / NANOSECONDS_PER_SECOND;
    return seconds;
  }

  private String printSummarySomeTimeUnit(long totalCpuTimeNanoSeconds,
      long totalUserTimeNanoSeconds, long totalSystemTimeNanoSeconds, String threadsString,
      java.util.function.Function<Long, Number> timeUnitConversionFunction, String timeUnitString) {
    String result = "";
    result += NL + "Total cpu time spent by " + threadsString + " (" + timeUnitString + "): "
        + timeUnitConversionFunction.apply(totalCpuTimeNanoSeconds);
    result += NL + "Total user time spent by " + threadsString + " (" + timeUnitString + "): "
        + timeUnitConversionFunction.apply(totalUserTimeNanoSeconds);
    result += NL + "Total system time spent by " + threadsString + " (" + timeUnitString + "): "
        + timeUnitConversionFunction.apply(totalSystemTimeNanoSeconds);
    return result;
  }

  private String printSummaryNanoSeconds(long totalCpuTimeNanoSeconds,
      long totalUserTimeNanoSeconds, long totalSystemTimeNanoSeconds, String threadsString) {
    return printSummarySomeTimeUnit(totalCpuTimeNanoSeconds, totalUserTimeNanoSeconds,
        totalSystemTimeNanoSeconds, threadsString, input -> input, "nanoseconds");
  }

  private String printSummarySeconds(long totalCpuTimeNanoSeconds, long totalUserTimeNanoSeconds,
      long totalSystemTimeNanoSeconds, String threadsString) {
    return printSummarySomeTimeUnit(totalCpuTimeNanoSeconds, totalUserTimeNanoSeconds,
        totalSystemTimeNanoSeconds, threadsString, input -> convertNanoSecondsToSeconds(input),
        "seconds");
  }

  private String headerCSVNanoSeconds(String threadsString) {
    String result = "";
    result += "Total_cpu_time_" + threadsString + "(ns)";
    result += CSV_FILE_SEPARATOR;
    result += "Total_user_time_" + threadsString + "(ns)";
    result += CSV_FILE_SEPARATOR;
    result += "Total_system_time_" + threadsString + "(ns)";
    return result;
  }

  private String headerCSVSeconds(String threadsString) {
    String result = "";
    result += "Total_cpu_time_" + threadsString + "(s)";
    result += CSV_FILE_SEPARATOR;
    result += "Total_user_time_" + threadsString + "(s)";
    result += CSV_FILE_SEPARATOR;
    result += "Total_system_time_" + threadsString + "(s)";
    return result;
  }

  private String summaryCSVNanoSeconds(long totalCpuTimeNanoSeconds, long totalUserTimeNanoSeconds,
      long totalSystemTimeNanoSeconds) {

    String result = "";
    result += totalCpuTimeNanoSeconds + "," + totalUserTimeNanoSeconds + ","
        + totalSystemTimeNanoSeconds;
    return result;
  }

  private String summaryCSVSeconds(long totalCpuTimeNanoSeconds, long totalUserTimeNanoSeconds,
      long totalSystemTimeNanoSeconds) {
    String result = "";
    result += DECIMAL_FORMAT.format(convertNanoSecondsToSeconds(totalCpuTimeNanoSeconds)) + ","
        + DECIMAL_FORMAT.format(convertNanoSecondsToSeconds(totalUserTimeNanoSeconds)) + ","
        + DECIMAL_FORMAT.format(convertNanoSecondsToSeconds(totalSystemTimeNanoSeconds));
    return result;
  }

  private String printSummary(long totalCpuTimeNanoSeconds, long totalUserTimeNanoSeconds,
      long totalSystemTimeNanoSeconds, String threadsString) {
    String result = "";
    result += printSummaryNanoSeconds(totalCpuTimeNanoSeconds, totalUserTimeNanoSeconds,
        totalSystemTimeNanoSeconds, threadsString);
    result += printSummarySeconds(totalCpuTimeNanoSeconds, totalUserTimeNanoSeconds,
        totalSystemTimeNanoSeconds, threadsString);
    return result;

  }

  /**
   * We use a functional reference here to avoid code duplication
   * 
   * @param headerCSVTimeUnitFunction
   * @return
   */
  private String headerCSV(java.util.function.Function<String, String> headerCSVTimeUnitFunction) {
    String result = "";
    result += headerCSVTimeUnitFunction.apply("all_threads");
    result += CSV_FILE_SEPARATOR;
    result += headerCSVTimeUnitFunction.apply("main_thread");
    result += CSV_FILE_SEPARATOR;
    result += headerCSVTimeUnitFunction.apply("all_except_main_threads");
    result += CSV_FILE_SEPARATOR;
    result += "wall_clock_time(ns)";
    return result;
  }

  private String headerCSVNanoSeconds() {
    return headerCSV(input -> headerCSVNanoSeconds(input));
  }

  private String headerCSVSeconds() {
    return headerCSV(input -> headerCSVSeconds(input));
  }

  /**
   * We use java 8's functional references to avoid code duplication, by providing a reference to a
   * summary generating function and a reference to time unit conversion function
   * 
   * @param summarySomeTimeUnitGenerationFunction
   * @param timeUnitConversionFunction
   * @return
   */
  private String summaryCSVSomeTimeUnit(
      NaryFunction<Long, String> summarySomeTimeUnitGenerationFunction,
      java.util.function.Function<Long, Number> timeUnitConversionFunction) {
    String result = "";
    result += summarySomeTimeUnitGenerationFunction.apply(getTotalCpuTimeSpentByAllThreads(),
        getTotalUserTimeSpentByAllThreads(), getTotalSystemTimeSpentByAllThreads());
    result += CSV_FILE_SEPARATOR;
    result += summarySomeTimeUnitGenerationFunction.apply(getTotalCpuTimeSpentByMainThread(),
        getTotalUserTimeSpentByMainThread(), getTotalSystemTimeSpentByMainThread());
    result += CSV_FILE_SEPARATOR;
    result += summarySomeTimeUnitGenerationFunction.apply(
        getTotalCpuTimeSpentByAllExceptMainThread(), getTotalUserTimeSpentByAllExceptMainThread(),
        getTotalSystemTimeSpentByAllExceptMainThread());
    result += CSV_FILE_SEPARATOR;
    result += timeUnitConversionFunction.apply(wallClockTimeDifference.getClockTimeSpent());

    return result;
  }

  private String summaryCSVNanoSeconds() {
    return summaryCSVSomeTimeUnit((arg1, arg2, arg3) -> summaryCSVNanoSeconds(arg1, arg2, arg3),
        input -> input);
  }

  private interface NaryFunction<T, T2> {
    public T2 apply(T arg1, T arg2, T arg3);
  }

  private String summaryCSVSeconds() {
    return summaryCSVSomeTimeUnit((arg1, arg2, arg3) -> summaryCSVSeconds(arg1, arg2, arg3),
        input -> convertNanoSecondsToSeconds(input));
  }

  /**
   * First register the end time of the main thread, then rgetTotalCpuTimeSpentByAllThreads(),
   * getTotalUserTimeSpentByAllThreads(), getTotalSystemTimeSpentByAllThreads()eport the time usage
   * for all threads and the total
   */
  public String registerEndTimeMainThreadAndReportThreadTimeUsage() {

    String result = "";
    registerThreadEndTimeMainThread();

    result += NL + "\n======================================================";
    result += NL + "============ ThreadCpuTimeKeeper Report ==============";
    result += NL + "======================================================";
   
    result += NL + "<Summary>";

    result += NL + "Total wall clock time spent (nanoseconds: "
        + wallClockTimeDifference.getClockTimeSpent();
    result += NL + "Total wall clock time spent (seconds): "
        + convertNanoSecondsToSeconds(wallClockTimeDifference.getClockTimeSpent());

    result += NL + "\n-----------------------------------------------------";

    result += printSummary(getTotalCpuTimeSpentByAllThreads(), getTotalUserTimeSpentByAllThreads(),
        getTotalSystemTimeSpentByAllThreads(), "all threads");

    result += NL + "\n-----------------------------------------------------";
    result += printSummary(getTotalCpuTimeSpentByMainThread(), getTotalUserTimeSpentByMainThread(),
        getTotalSystemTimeSpentByMainThread(), "main thread");

    result += NL + "\n-----------------------------------------------------";
    result += printSummary(getTotalCpuTimeSpentByAllExceptMainThread(),
        getTotalUserTimeSpentByAllExceptMainThread(),
        getTotalSystemTimeSpentByAllExceptMainThread(), "all except main threads");
    result += NL + "\n-----------------------------------------------------";
    result += NL + "Summary in csv format: \n";

    result += NL + headerCSVNanoSeconds();
    result += NL + summaryCSVNanoSeconds();
    result += NL + headerCSVSeconds();
    result += NL + summaryCSVSeconds();

    result += NL + "</Summary>";

    
    result += NL + NL + "<Details>";
    
    result += NL + "Time used by main thread:\n" + threadIDToCpuTimingMap.get(mainThreadID) + "\n";

    result += NL + "Time used by other threads:";
    for (Entry<Long, ThreadCpuTimeStampDifference> entry : threadIDToCpuTimingMap.entrySet()) {
      long threadId = entry.getKey();
      if (threadId != mainThreadID) {
        result += NL + entry.getValue();
      }
    }

    result += NL + "</Details>";
    
    result += NL + "\n======================================================";
    result += NL + "======================================================";
    result += NL + "======================================================";

    return result;

  }

  public void registerEndTimeMainThreadAndReportThreadTimeUsageToFile(String filePath) {
    String reportString = registerEndTimeMainThreadAndReportThreadTimeUsage();

    // Write the report to the output file
    // Use try-with-resources to ensure the writer will be closed in the
    // end, without explicitly doing this manually
    try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filePath));) {
      outputWriter.write(reportString);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  // Functional references for getCpuTime, getSystemTime and getUserTime
  private static final ThreadCpuTimeDifferenceGetTimeOperation getCpuTimeOperation = (
      ThreadCpuTimeStampDifference threadCpuTimeStampDifference) -> threadCpuTimeStampDifference
          .getCpuTimeSpent();
  private static final ThreadCpuTimeDifferenceGetTimeOperation getSystemTimeOperation = (
      ThreadCpuTimeStampDifference threadCpuTimeStampDifference) -> threadCpuTimeStampDifference
          .getSystemTimeSpent();
  private static final ThreadCpuTimeDifferenceGetTimeOperation getUserTimeOperation = (
      ThreadCpuTimeStampDifference threadCpuTimeStampDifference) -> threadCpuTimeStampDifference
          .getUserTimeSpent();

  private interface ThreadCpuTimeDifferenceGetTimeOperation {
    long getTime(ThreadCpuTimeStampDifference threadCpuTimeStampDifference);
  }

  private static class WallClockTimeDifference {
    private final long clockStartTime;
    private long clockEndTime = -1;

    private WallClockTimeDifference(long clockStartTime) {
      this.clockStartTime = clockStartTime;
    }

    private static WallClockTimeDifference createWallClockTimeDifference() {
      long clockStartTime = System.nanoTime();
      return new WallClockTimeDifference(clockStartTime);
    }

    private void registerWallClockEndTime() {
      this.clockEndTime = System.nanoTime();
    }

    private long getClockTimeSpent() {
      return clockEndTime - clockStartTime;
    }

  }

  /**
   * The ThreadCpuTimeStamp class stores userTime and cpuTime
   * 
   * User Time - This is the time the CPU spent running your code. It is called user time because
   * the CPU is used by an operation in a program that a user has started.
   * 
   * System time - This is the time that the CPU was used for executing system calls. It is
   * literally the time the kernel is using the CPU for its operations. You can think of I/O
   * operations, context switches, inter process communication, memory management, interrupt
   * requests, etc.
   *
   *
   * systemTime is userTime + cpuTime, there is no way to get system time directly
   * 
   * @author gemaille
   *
   */
  private static class ThreadCpuTimeStamp {
    private final long userTime;
    private final long cpuTime;

    private ThreadCpuTimeStamp(long userTime, long cpuTime) {
      this.userTime = userTime;
      this.cpuTime = cpuTime;
    }

    /**
     * The start time method reads the cpu time first and the user time last. We do this because we
     * want to assure the passed cpu time will be larger than the passed user time. Ideally we would
     * like to read both simultaneously, but that appears to be not possible.
     * 
     * @param thread
     * @return
     */
    private static ThreadCpuTimeStamp createThreadBeginTimeStampForThread(Thread thread) {
      long cpuTime = getCpuTime(thread);
      long userTime = getUserTime(thread);
      // System.out.println("userTime: " + userTime);
      // System.out.println("cpuTime: " + cpuTime);
      return new ThreadCpuTimeStamp(userTime, cpuTime);
    }

    /**
     * The end time method reads the user time first and the cpu time last. We do this because we
     * want to assure the passed cpu time will be larger than the passed user time. Ideally we would
     * like to read both simultaneously, but that appears to be not possible.
     * 
     * @param thread
     * @return
     */
    private static ThreadCpuTimeStamp createThreadEndTimeStampForThread(Thread thread) {
      long userTime = getUserTime(thread);
      long cpuTime = getCpuTime(thread);
      // System.out.println("userTime: " + userTime);
      // System.out.println("cpuTime: " + cpuTime);
      return new ThreadCpuTimeStamp(userTime, cpuTime);
    }

    // See:
    // http://nadeausoftware.com/articles/2008/03/java_tip_how_get_cpu_and_user_time_benchmarking
    public long getSystemTime() {
      return cpuTime - userTime;
    }
  }

  private static class ThreadCpuTimeStampDifference {
    private final long threadId;
    private final ThreadCpuTimeStamp startTimeStap;
    private ThreadCpuTimeStamp endTimeStap;

    private ThreadCpuTimeStampDifference(long threadId, ThreadCpuTimeStamp startTimeStap) {
      this.threadId = threadId;
      this.startTimeStap = startTimeStap;
      this.endTimeStap = null;
    }

    private static ThreadCpuTimeStampDifference createThreadCpuTimeStampDifference(Thread thread) {
      ThreadCpuTimeStamp startTimeStamp = ThreadCpuTimeStamp
          .createThreadBeginTimeStampForThread(thread);
      return new ThreadCpuTimeStampDifference(thread.getId(), startTimeStamp);
    }

    public void registerCpuEndTimeStamp(Thread thread) {
      if (thread.getId() != threadId) {
        throw new RuntimeException(
            "ThreadCpuTimeStampDifference - Error: trying to register cpu end time stamp for the wrong thread");
      }
      ThreadCpuTimeStamp endTimeStamp = ThreadCpuTimeStamp
          .createThreadEndTimeStampForThread(thread);
      this.endTimeStap = endTimeStamp;
    }

    public long getCpuTimeSpent() {
      return endTimeStap.cpuTime - startTimeStap.cpuTime;
    }

    public long getUserTimeSpent() {
      return endTimeStap.userTime - startTimeStap.userTime;
    }

    public long getSystemTimeSpent() {
      return endTimeStap.getSystemTime() - startTimeStap.getSystemTime();
    }

    public String toString() {
      String result = "\n<ThreadCpuTimeStampDifference>";
      result += "\nThread ID: " + threadId;
      result += "\nCPU start time: " + startTimeStap.cpuTime;
      result += "\nCPU end time: " + endTimeStap.cpuTime;
      result += "\nUser start time: " + startTimeStap.userTime;
      result += "\nUser end time: " + endTimeStap.userTime;
      result += "\nSystem start time: " + startTimeStap.getSystemTime();
      result += "\nSystem end time: " + endTimeStap.getSystemTime();
      result += "\n-----------------------------------";
      result += "\nCPU time  spent: " + getCpuTimeSpent();
      result += "\nUser time spent: " + getUserTimeSpent();
      result += "\nSystem time spent: " + getSystemTimeSpent();
      result += "\n<\\ThreadCpuTimeStampDifference>\n";
      return result;
    }

  }

}
