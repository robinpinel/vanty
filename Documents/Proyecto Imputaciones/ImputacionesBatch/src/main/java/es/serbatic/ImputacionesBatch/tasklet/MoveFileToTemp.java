package es.serbatic.ImputacionesBatch.tasklet;

import java.nio.file.Path;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import es.serbatic.ImputacionesBatch.utilities.FileUtilities;

@Component
@PropertySource("classpath:error.properties")
@PropertySource("classpath:files.properties")
public class MoveFileToTemp implements Tasklet{

    @Value("${base.directory}")
    private String inputDirectory;

    @Value("${work.directory}")
    private String workDirectory;
    
    @Value("${files.maxAttempts}")
    private String maxAttempts;
    
    @Value("${files.delayInMillis}")
    private String delayInMillis;
    
    @Value("${GO_TO_END}")
    private String GO_TO_END;
    
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		boolean bSuccess;
		
		System.out.println("Tasklet para mover el fichero original a un directorio de trabajo temporal");
		System.out.println("Directorio remoto: " + inputDirectory);
		System.out.println("Directorio de trabajo: " + workDirectory);
		System.out.println("Fichero que hay que mover: " + (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("fileName"));
		
		String fileName = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("fileName");
		Path pathSource = Path.of(fileName);
		Path pathTemp = Path.of(workDirectory);
    	
		Integer maxAttemps = (Integer) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("filesMaxAttemps");
    	Integer delay = (Integer) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("filesDelayInMillis");

		
		bSuccess = FileUtilities.moveFileWithRetries(pathSource, pathTemp, maxAttemps, delay);
		
		if(!bSuccess) {
			contribution.setExitStatus(new ExitStatus(GO_TO_END));
		} else {
			chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("fileNameTemp", pathTemp.toString() + "\\" + pathSource.getFileName());
		//	chunkContext.getStepContext().getStepExecution().getExecutionContext().put("fileNameTemp", pathTemp.toString());
			chunkContext.getStepContext().getStepExecution().getExecutionContext().put("fileNameTemp", pathTemp.toString() + "\\" + pathSource.getFileName());
		}
		
		return RepeatStatus.FINISHED;
	}
}
