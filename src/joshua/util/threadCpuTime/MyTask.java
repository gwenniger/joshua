package joshua.util.threadCpuTime;


public class MyTask implements Runnable{
    
    private final ThreadCpuTimer threadCpuTimer;
    private String name;
    private int count;
    public MyTask(String name, int count,ThreadCpuTimer threadCpuTimer) {
        this.name = name;
        this.count = count;
        this.threadCpuTimer = threadCpuTimer;
    }
    public void run() {
  threadCpuTimer.startThreadCpuTimer(Thread.currentThread());
        String[] strArray = new String[count];
        System.out.println("Start MyTask (" + name + "), loop count " + count);
        for (int i = 0; i < count; i++) {
            strArray[i]= "String " + (i * 10);
        }
        StringBuffer sb = new StringBuffer();
        int i = 0;
        for (String str : strArray) {
            sb.append(str).append("\n");
            i++;
            if (i == count/2) {
                //sleep so that main thread can catch up to calculate the cpu times 
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }      
        System.out.println(name + " done");
        threadCpuTimer.stopThreadCpuTimer(Thread.currentThread());
    }
 
}