package joshua.util.threadCpuTime;

/**
 * This class implements ThreadCpuTimer, but doesn't do anything.
 * This can be used when no timing is desired.
 * @author gemaille
 *
 */
public class ThreadCpuTimeKeeperBogus implements ThreadCpuTimer{

  
  public static ThreadCpuTimeKeeperBogus createThreadCpuTimeKeeperBogus(){
    return new ThreadCpuTimeKeeperBogus();
  }
  
  @Override
  public void startThreadCpuTimer(Thread thread) {
  }

  @Override
  public void stopThreadCpuTimer(Thread thread) {
  }

}
