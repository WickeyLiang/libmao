package net.andrewmao.models.randomutility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of Azari, Parkes, Xia paper on RUM
 * using multithreading
 * 
 * @author mao
 *
 * @param <T>
 */
public abstract class MCEMModel<T> extends RandomUtilityModel<T> {
	
	List<int[]> rankings;
		
	int iterations;
	double[] start;
	
	ExecutorService exec;
	CountDownLatch latch;
	
	protected MCEMModel(List<T> items) {
		super(items);		
				
		rankings = new ArrayList<int[]>();	
	}
	
	public void addData(List<T> list) {
		int[] ranking = new int[list.size()];		
		int i = 0;
		for( T item : list ) ranking[i++] = items.indexOf(item) + 1;		
		rankings.add(ranking);
	}
	
	public void addData(T[] arr) {
		int[] ranking = new int[arr.length];		
		int i = 0;
		for( T item : arr ) ranking[i++] = items.indexOf(item) + 1;		
		rankings.add(ranking);
		
		System.out.println("Added " + Arrays.toString(ranking));
	}
	
	public void setup(int iterations, double[] startPoint) {
		this.iterations = iterations;
		this.start = startPoint;
	}
	
	/*
	 * Implemented by subclasses
	 */
	protected abstract void initialize();	
	protected abstract void eStep(int iter);	
	protected abstract void mStep();
	protected abstract double[] getCurrentParameters();
	
	@Override
	public synchronized double[] getParameters() {
		exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		initialize();
		
		for( int i = 0; i < iterations; i++ ) {
			eStep(i);
			
			// Wait for sampling to finish
			while( latch.getCount() > 0 ) {
				try { latch.await();
				} catch (InterruptedException e) {}				
			}
			
			mStep();
		}
		
		exec.shutdown();
		
		return getCurrentParameters();
	}

	void beginNumJobs(int size) {
		latch = new CountDownLatch(size);	
	}

	void addJob(Callable<?> job) {
		exec.submit(job);		
	}

	void finishJob() {
		latch.countDown();
	}
}
