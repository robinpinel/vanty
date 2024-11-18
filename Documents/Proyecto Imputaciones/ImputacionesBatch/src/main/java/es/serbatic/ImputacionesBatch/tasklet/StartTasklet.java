package es.serbatic.ImputacionesBatch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:files.properties")
public class StartTasklet implements Tasklet {

    @Value("${files.maxAttempts}")
    private Integer maxAttempts;
    
    @Value("${files.delayInMillis}")
    private Integer delayInMillis;
    
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		System.out.println("Estoy en el step 1");

		chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("filesMaxAttemps", maxAttempts);
		chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("filesDelayInMillis", delayInMillis);
		
		return RepeatStatus.FINISHED;
	}

}
