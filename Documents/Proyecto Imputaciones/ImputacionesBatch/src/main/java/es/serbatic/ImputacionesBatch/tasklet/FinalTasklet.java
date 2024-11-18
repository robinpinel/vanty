package es.serbatic.ImputacionesBatch.tasklet;

import java.nio.file.Path;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import es.serbatic.ImputacionesBatch.utilities.FileUtilities;

@Component
@PropertySource("classpath:files.properties")
public class FinalTasklet implements Tasklet {
    
    @Value("${processed.directory}")
    private String processedDirectory;
    
    @Value("${rejected.directory}")
    private String rejectedDirectory;
    
   
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		System.out.println("Estoy en el step final");
		String sStateFolder = processedDirectory;
		String sFileName;
		Path pathStateFolder;
		Path pathFileName;;
		
        boolean hasFailedStep = chunkContext.getStepContext().getStepExecution()
                .getJobExecution()
                .getStepExecutions()
                .stream()
                .anyMatch(step -> step.getExitStatus().getExitCode().equals("FAILED"));
        
        String noFiles = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("NO_FILES_TO_PROCESS");
        
        if(noFiles == null) {
        	sFileName = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("fileNameTemp");
        	Integer maxAttemps = (Integer) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("filesMaxAttemps");
        	Integer delay = (Integer) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("filesDelayInMillis");
        	
        	pathFileName = Path.of(sFileName);
	        
        	if(hasFailedStep) {
	        	System.out.println("El JOB ha finalizado con error");
	        	sStateFolder = rejectedDirectory;
	        } else {
	        	System.out.println("El JOB ha terminado de manera correcta");
	        }
	        
	        pathStateFolder = Path.of(sStateFolder);
	        
	        FileUtilities.moveFileWithRetries(pathFileName, pathStateFolder, maxAttemps, delay);
        } else {
        	System.out.println("Se pasa por el step final sin haber procesado ficheros");
        }
        
		return RepeatStatus.FINISHED;
	}

}
