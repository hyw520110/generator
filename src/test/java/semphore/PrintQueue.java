package semphore;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * semaphore含义
信号量就是可以声明多把锁（包括一把锁：此时为互斥信号量）。
举个例子：一个房间如果只能容纳5个人，多出来的人必须在门外面等着。如何去做呢？一个解决办法就是：房间外面挂着五把钥匙，每进去一个人就取走一把钥匙，没有钥匙的不能进入该房间而是在外面等待。每出来一个人就把钥匙放回原处以方便别人再次进入。
 *
 *常用方法
acquire():获取信号量，信号量内部计数器减1
release():释放信号量，信号量内部计数器加1
tryAcquire():这个方法试图获取信号量，如果能够获取返回true，否则返回false
信号量控制的线程数量在声明时确定。例如：
Semphore s = new Semphore(2);
需要注意的地方
1、对于信号量声明的临界区，虽然可以控制线程访问的数量，但是不能保证代码块之间是线程安全的。所以上面的例子在方法printJob()方法里面使用了锁保证数据安全性。
2、信号量也涉及到公平性问题。和锁公平性一样，这里默认是非公平的。可以通过构造器显示声明锁的公平性。
public Semaphore(int permits, boolean fair)
应用场景
流量控制，即控制能够访问的最大线程数。
实现一个功能：一个打印队列，被三台打印机打印
 */
public class PrintQueue {
    private Semaphore semaphore;
    private boolean freePrinters[];
    private Lock lockPrinters;
    
    public PrintQueue(){
        semaphore=new Semaphore(3);
        freePrinters=new boolean[3];
        for (int i=0; i<3; i++){
            freePrinters[i]=true;
        }
        lockPrinters=new ReentrantLock();
    }
    public static void Main (String args[]){
        PrintQueue printQueue=new PrintQueue();
        Thread thread[]=new Thread[12];
        for (int i=0; i<12; i++){
            thread[i]=new Thread(new Job(printQueue),"Thread "+i);
        }
        for (int i=0; i<12; i++){
            thread[i].start();
        }
    }
    public void printJob (Object document){
        try {
            semaphore.acquire();
            
            int assignedPrinter=getPrinter();
            
            Long duration=(long)(Math.random()*10);
            System.out.printf("%s: PrintQueue: Printing a Job in Printer %d during %d seconds\n",Thread.currentThread().getName(),assignedPrinter,duration);
            TimeUnit.SECONDS.sleep(duration);
            
            freePrinters[assignedPrinter]=true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Free the semaphore
            semaphore.release();            
        }
    }
    private int getPrinter() {
        int ret=-1;
        
        try {
            lockPrinters.lock();
            for (int i=0; i<freePrinters.length; i++) {
                if (freePrinters[i]){
                    ret=i;
                    freePrinters[i]=false;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lockPrinters.unlock();
        }
        return ret;
    }
}