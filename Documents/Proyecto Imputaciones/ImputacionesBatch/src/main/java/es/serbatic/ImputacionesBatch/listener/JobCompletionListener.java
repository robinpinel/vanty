package es.serbatic.ImputacionesBatch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionListener implements JobExecutionListener {

	@Override
	public void beforeJob(JobExecution jobExecution) {
		JobExecutionListener.super.beforeJob(jobExecution);
		System.out.println("Antes de comenzar el Job");
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		JobExecutionListener.super.afterJob(jobExecution);
		System.out.println("Después de finalizar el Job");
	}

	
}
