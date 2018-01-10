package priv.cais.jedis;
/**
 * @auther CaiS
 */
public class TaskShedulerSystem {
	public static void main(String[] args) throws Exception {

		new Thread(new TaskProducer()).start();

		Thread.sleep(15000);

		new Thread(new TaskConsumer()).start();

		Thread.sleep(Long.MAX_VALUE);
	}
}
