package phaser;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
/**
 * Phaser

含义
更加复杂和强大的同步辅助类。它允许并发执行多阶段任务。当我们有并发任务并且需要分解成几步执行时，（CyclicBarrier是分成两步），就可以选择使用Phaser。Phaser类机制是在每一步结束的位置对线程进行同步，当所有的线程都完成了这一步，才允许执行下一步。
跟其他同步工具一样，必须对Phaser类中参与同步操作的任务数进行初始化，不同的是，可以动态的增加或者减少任务数。
 
函数
arriveAndAwaitAdvance():类似于CyclicBarrier的await()方法，等待其它线程都到来之后同步继续执行
arriveAndDeregister()：把执行到此的线程从Phaser中注销掉
isTerminated():判断Phaser是否终止
register():将一个新的参与者注册到Phaser中，这个新的参与者将被当成没有执行完本阶段的线程
forceTermination():强制Phaser进入终止态
... ...
 
例子
使用Phaser类同步三个并发任务。这三个任务将在三个不同的文件夹及其子文件夹中查找过去24小时内修改过扩展为为.log的文件。这个任务分成以下三个步骤：
1、在执行的文件夹及其子文件夹中获取扩展名为.log的文件
2、对每一步的结果进行过滤，删除修改时间超过24小时的文件
3、将结果打印到控制台
在第一步和第二步结束的时候，都会检查所查找到的结果列表是不是有元素存在。如果结果列表是空的，对应的线程将结束执行，并从Phaser中删除。（也就是动态减少任务数）
文件查找类

注意的地方
例子中Phaser分了三个步骤：查找文件、过滤文件、打印结果。并且在查找文件和过滤文件结束后对结果进行分析，如果是空的，将此线程从Phaser中注销掉。也就是说，下一阶段，该线程将不参与运行。
在run()方法中，开头调用了phaser的arriveAndAwaitAdvance()方法来保证所有线程都启动了之后再开始查找文件。在查找文件和过滤文件阶段结束之后，都对结果进行了处理。即：如果结果是空的，那么就把该条线程移除，如果不空，那么等待该阶段所有线程都执行完该步骤之后在统一执行下一步。最后，任务执行完后，把Phaser中的线程均注销掉。
Phaser其实有两个状态：活跃态和终止态。当存在参与同步的线程时，Phaser就是活跃的。并且在每个阶段结束的时候同步。当所有参与同步的线程都取消注册的时候，Phase就处于终止状态。在这种状态下，Phaser没有任务参与者。
Phaser主要功能就是执行多阶段任务，并保证每个阶段点的线程同步。在每个阶段点还可以条件或者移除参与者。主要涉及方法arriveAndAwaitAdvance()和register()和arriveAndDeregister()
 
使用场景
多阶段任务

 *
 */
public class FileSearch implements Runnable {
	private String initPath;

	private String end;

	private List<String> results;

	private Phaser phaser;

	public FileSearch(String initPath, String end, Phaser phaser) {
		this.initPath = initPath;
		this.end = end;
		this.phaser = phaser;
		results = new ArrayList<>();
	}
	 public static void main(String[] args) {
		Phaser phaser = new Phaser(3);

		FileSearch system = new FileSearch("C:\\Windows", "log", phaser);
		FileSearch apps = new FileSearch("C:\\Program Files", "log", phaser);
		FileSearch documents = new FileSearch("C:\\Documents And Settings", "log", phaser);

		Thread systemThread = new Thread(system, "System");
		systemThread.start();
		Thread appsThread = new Thread(apps, "Apps");
		appsThread.start();
		Thread documentsThread = new Thread(documents, "Documents");
		documentsThread.start();
		try {
			systemThread.join();
			appsThread.join();
			documentsThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.printf("Terminated: %s\n", phaser.isTerminated());
    }
	@Override
	public void run() {

		phaser.arriveAndAwaitAdvance();// 等待所有的线程创建完成，确保在进行文件查找的时候所有的线程都已经创建完成了

		System.out.printf("%s: Starting.\n", Thread.currentThread().getName());

		// 1st Phase: 查找文件
		File file = new File(initPath);
		if (file.isDirectory()) {
			directoryProcess(file);
		}

		// 如果查找结果为false，那么就把该线程从Phaser中移除掉并且结束该线程的运行
		if (!checkResults()) {
			return;
		}

		// 2nd Phase: 过滤结果，过滤出符合条件的（一天内的）结果集
		filterResults();

		// 如果过滤结果集结果是空的，那么把该线程从Phaser中移除，不让它进入下一阶段的执行
		if (!checkResults()) {
			return;
		}

		// 3rd Phase: 显示结果
		showInfo();
		phaser.arriveAndDeregister();// 任务完成，注销掉所有的线程
		System.out.printf("%s: Work completed.\n", Thread.currentThread().getName());
	}

	private void showInfo() {
		for (int i = 0; i < results.size(); i++) {
			File file = new File(results.get(i));
			System.out.printf("%s: %s\n", Thread.currentThread().getName(), file.getAbsolutePath());
		}
		// Waits for the end of all the FileSearch threads that are registered in the
		// phaser
		phaser.arriveAndAwaitAdvance();
	}

	private boolean checkResults() {
		if (results.isEmpty()) {
			System.out.printf("%s: Phase %d: 0 results.\n", Thread.currentThread().getName(), phaser.getPhase());
			System.out.printf("%s: Phase %d: End.\n", Thread.currentThread().getName(), phaser.getPhase());
			// 结果为空，Phaser完成并把该线程从Phaser中移除掉
			phaser.arriveAndDeregister();
			return false;
		} else {
			// 等待所有线程查找完成
			System.out.printf("%s: Phase %d: %d results.\n", Thread.currentThread().getName(), phaser.getPhase(),
					results.size());
			phaser.arriveAndAwaitAdvance();
			return true;
		}
	}

	private void filterResults() {
		List<String> newResults = new ArrayList<>();
		long actualDate = new Date().getTime();
		for (int i = 0; i < results.size(); i++) {
			File file = new File(results.get(i));
			long fileDate = file.lastModified();

			if (actualDate - fileDate < TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)) {
				newResults.add(results.get(i));
			}
		}
		results = newResults;
	}

	private void directoryProcess(File file) {
		// Get the content of the directory
		File list[] = file.listFiles();
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				if (list[i].isDirectory()) {
					// If is a directory, process it
					directoryProcess(list[i]);
				} else {
					// If is a file, process it
					fileProcess(list[i]);
				}
			}
		}
	}

	private void fileProcess(File file) {
		if (file.getName().endsWith(end)) {
			results.add(file.getAbsolutePath());
		}
	}
}