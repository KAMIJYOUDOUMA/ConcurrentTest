package priv.cais;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntergerTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		AtomicInteger ai=new AtomicInteger();
		System.out.println(ai.get());
		System.out.println(ai.incrementAndGet());
		System.out.println(ai.getAndIncrement());
		System.out.println(ai.getAndAdd(11));
		System.out.println(ai.get());

		Integer i=new Integer(1);
	}

}
