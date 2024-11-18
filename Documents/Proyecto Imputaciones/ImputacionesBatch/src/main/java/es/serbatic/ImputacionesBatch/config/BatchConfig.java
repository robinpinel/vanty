package es.serbatic.ImputacionesBatch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import es.serbatic.ImputacionesBatch.dto.ImputacionDTO;
import es.serbatic.ImputacionesBatch.listener.CustomJobListener;
import es.serbatic.ImputacionesBatch.listener.CustomStepListener;
import es.serbatic.ImputacionesBatch.listener.JobCompletionListener;
import es.serbatic.ImputacionesBatch.service.FileProcessingService;
import es.serbatic.ImputacionesBatch.tasklet.FinalTasklet;
import es.serbatic.ImputacionesBatch.tasklet.MoveFileToTemp;
import es.serbatic.ImputacionesBatch.tasklet.StartTasklet;


@Configuration
@EnableBatchProcessing
@PropertySource("classpath:error.properties")
public class BatchConfig {

    @Value("${GO_TO_END}")
    private String GO_TO_END;
    
    @Value("${NO_FILES}")
    private String NO_FILES;
	
    @Autowired
    private DataSource dataSource;
    
	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final FileProcessingService fileProcessingService;
	private final StartTasklet startTasklet;
	private final FinalTasklet finalTasklet;
	private final MoveFileToTemp moveFileToTempTasklet;
	
	@Autowired
	private CustomJobListener customJobListener;
	
	@Autowired
	private CustomStepListener customStepListener;
	
    public BatchConfig(JobRepository jobRepository, 
    				   PlatformTransactionManager transactionManager, 
    				   FileProcessingService fileProcessingService,
    				   StartTasklet startTasklet,
    				   FinalTasklet finalTasklet,
    				   MoveFileToTemp moveFileToTemp) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.fileProcessingService = fileProcessingService;
        this.startTasklet = startTasklet;
        this.finalTasklet = finalTasklet;
        this.moveFileToTempTasklet = moveFileToTemp;
    }
	
    @Bean
    JobCompletionListener jobCompletionListener() {		//define un listener para un job
        return new JobCompletionListener();
    }
    
	@Bean
    Job processJob() {
        return new JobBuilder("processJob", jobRepository)
        		.listener(jobCompletionListener())
        		.listener(customJobListener)
                .start(startStep())
                .next(validationStep()).on(NO_FILES).to(finalStep())
                .from(validationStep()) 
                .on("*").to(moveFileToTempStep())
                .on(GO_TO_END).to(finalStep())
                .from(moveFileToTempStep())
                .next(processingStep()).on("FAILED").to(finalStep())
                .from(processingStep())
                .on("*").to(finalStep())
                .end()
                .build();
    }
	
	@Bean
	Step startStep() {
        return new StepBuilder("startStep", jobRepository)
                .tasklet(startTasklet, transactionManager)
                .listener(customStepListener)
                .build();
	}
	
	/**
	 * Step que realizará ciertas validaciones previas para dar comienzo con todo el funcionamiento del batch
	 * Validación de estructura de directorios
	 * @return
	 */
    @Bean
    Step validationStep() {
        return new StepBuilder("validationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    fileProcessingService.validateOrCreateDirectories();  // Valida o crea los directorios
                    fileProcessingService.hasFilesMatchingPattern(chunkContext, contribution);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
    
    /**
     * Step que permite llevarnos un archivo desde el buzón de entrada hasta un directorio de trabajo temporal
     * Será en ese direcotrio temporal donde el fichero será procesado
     * @return
     */
	@Bean
	Step moveFileToTempStep() {
        return new StepBuilder("moveFileToTempStep", jobRepository)
                .tasklet(moveFileToTempTasklet, transactionManager)
                .listener(customStepListener)
                .build();
	}
	
	/**
	 * Step que permitirá el procesamiento del archivo obtenido del buzón de entrada
	 * @return
	 */	
	@Bean
	Step processingStep() {
        return new StepBuilder("processingStep", jobRepository)
                .<ImputacionDTO, ImputacionDTO>chunk(10, transactionManager)
                .reader(defaultReader())
                .processor(processor())
                .writer(writer(dataSource))
                .build();
	}
	
    /**
     * Step que realizará las operaciones finales, como por ejemplo llevarnos el fichero procesado a un directorio
     * de procesados o rechazados.
     * También se realizarán los avisos oprtunos mediante envío de correo electrónico.
     * @return
     */
    @Bean
    Step finalStep() {
        return new StepBuilder("finalStep", jobRepository)
                .tasklet(finalTasklet, transactionManager)
                .listener(customStepListener)
                .build();
    }
    
    @Bean
    @StepScope
    FlatFileItemReader<ImputacionDTO> defaultReader() {
        return reader(null);
    }
    
    @Bean
    @StepScope
    FlatFileItemReader<ImputacionDTO> reader(@Value("#{jobExecutionContext['fileNameTemp']}") String tempFilePath) {
        FlatFileItemReader<ImputacionDTO> reader = new FlatFileItemReader<>();
        
        reader.setResource(new FileSystemResource(tempFilePath));
        reader.setLineMapper(new DefaultLineMapper<ImputacionDTO>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setDelimiter("|");
                setNames("PEP", "n_personal", "fecha", "cantidad"); // Columnas del CSV
                setStrict(false);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<ImputacionDTO>() {{
                setTargetType(ImputacionDTO.class);
            }});
        }});
        
        System.out.println("Estoy en el reader");
        
        return reader;
    }
    
    @Bean
    ItemProcessor<ImputacionDTO, ImputacionDTO> processor() {
        return record -> {
            System.out.println("Estoy en el processor");
            System.out.println("Registro: " + record.getPEP() + " - " + record.getN_personal() + " - " + 
            		record.getFecha() + " - " + record.getCantidad());
            return record;
        };
    }
    
    @Bean
    JdbcBatchItemWriter<ImputacionDTO> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ImputacionDTO>()
                .dataSource(dataSource)
                .sql("INSERT INTO imputaciones (denominacionPEP, n_pers, ce_coste, fecha, cantidad) VALUES (:PEP, :n_personal, :PEP, :fecha, :cantidad)")
                .beanMapped()
                .build();
    }
}
