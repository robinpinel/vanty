package es.serbatic.ImputacionesBatch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class CustomJobListener implements JobExecutionListener {

	private CustomStepListener stepListener;
	
	public CustomJobListener(CustomStepListener stepListener) {
		this.stepListener = stepListener;
	}
	
	@Override
	public void beforeJob(JobExecution jobExecution) {
		// TODO Auto-generated method stub
		JobExecutionListener.super.beforeJob(jobExecution);
		System.out.println("Job " + jobExecution.getJobInstance().getJobName() + " is starting.");
		System.out.println("Antes de que comience el Job");
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		// TODO Auto-generated method stub
		JobExecutionListener.super.afterJob(jobExecution);
		System.out.println("Última cosa antes de que finalice el Job");
	}

	
}
