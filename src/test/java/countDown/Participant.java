package countDown;

import java.util.concurrent.TimeUnit;

public class Participant implements Runnable {
	private VideoConference conference;
	private String name;

	public Participant(VideoConference conference, String name) {
		this.conference = conference;
		this.name = name;
	}

	@Override
	public void run() {
		Long duration = (long) (Math.random() * 10);
		try {
			TimeUnit.SECONDS.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		conference.arrive(name);// 每到一个人员，CountDownLatch计数器就减少1
	}
}