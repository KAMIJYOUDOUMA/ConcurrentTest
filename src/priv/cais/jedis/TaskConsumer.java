package priv.cais.jedis;
/**
 * @auther CaiS
 */
import java.util.Random;

import redis.clients.jedis.Jedis;

public class TaskConsumer implements Runnable {
	Jedis jedis = new Jedis(Consts.REDISHOST,Consts.PORT);

	public void run() {
		Random random = new Random();
		while(true){
			String taskid = jedis.rpoplpush(Consts.TASKQUEUE, Consts.TMPQUEUE);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(random.nextInt(13) % 7 == 0){
				jedis.rpoplpush(Consts.TMPQUEUE, Consts.TASKQUEUE);
				System.out.println(taskid + "处理失败，被弹回任务队列");
			} else {
				jedis.rpop(Consts.TMPQUEUE);
				System.out.println(taskid+"处理成功，被清除");
			}
		}

	}



}
