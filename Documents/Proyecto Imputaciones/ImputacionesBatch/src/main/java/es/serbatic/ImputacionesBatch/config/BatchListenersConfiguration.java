package es.serbatic.ImputacionesBatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import es.serbatic.ImputacionesBatch.listener.CustomJobListener;
import es.serbatic.ImputacionesBatch.listener.CustomStepListener;

@Configuration
public class BatchListenersConfiguration {

	@Bean
	CustomJobListener customJobListener(CustomStepListener stepListener) {
        return new CustomJobListener(stepListener);
    }
	
	@Bean
	CustomStepListener customStepListener() {
		return new CustomStepListener();
	}
}
