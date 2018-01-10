package priv.cais.jedis;
/**
 * @auther CaiS
 */
import java.util.Random;
import java.util.UUID;

import redis.clients.jedis.Jedis;

public class TaskProducer implements Runnable{
    Jedis jedis = new Jedis(Consts.REDISHOST,Consts.PORT);

	public void run() {
		Random random = new Random();
		while(true){
			try{
				Thread.sleep(random.nextInt(600) + 600);
				UUID taskid = UUID.randomUUID();
				jedis.lpush(Consts.TASKQUEUE, taskid.toString());
				System.out.println("插入了一个新的任务： " + taskid);
			}catch(Exception e){
				e.printStackTrace();
			}
		}

	}

}
