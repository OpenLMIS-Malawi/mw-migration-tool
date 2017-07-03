package mw.gov.health.lmis.migration.tool.batch;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate.DEFAULT_THROTTLE_LIMIT;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import mw.gov.health.lmis.migration.tool.config.ToolBatchConfiguration;
import mw.gov.health.lmis.migration.tool.config.ToolProperties;
import mw.gov.health.lmis.migration.tool.openlmis.BaseRequisition;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;
import mw.gov.health.lmis.migration.tool.scm.domain.Main;

import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

  @Bean
  public AppBatchConfigurer batchConfigurer() {
    return new AppBatchConfigurer();
  }

  @Bean
  public BatchDatabaseInitializer batchDatabaseInitializer(ApplicationContext applicationContext,
                                                           BatchProperties batchProperties) {
    return new AppBatchDatabaseInitializer(batchConfigurer(), applicationContext, batchProperties);
  }

  /**
   * Configure Spring Batch Step that will read {@link Main} object, convert it into
   * {@link Requisition} object and save it into OpenLMIS database.
   */
  @Bean
  public Step migrationStep(StepBuilderFactory stepBuilderFactory,
                            MainReader reader,
                            MainReadListener readerListener,
                            RequisitionWriter writer,
                            RequisitionWriteListener writerListener,
                            MigrationProcessor processor,
                            MigrationProcessListener processorListener,
                            ToolProperties toolProperties)
      throws IllegalAccessException, InstantiationException {
    ToolBatchConfiguration batchProperties = toolProperties
        .getConfiguration()
        .getBatch();

    return stepBuilderFactory
        .get("migrationStep")
        .<Main, List<Requisition>>chunk(batchProperties.getChunk())
        .reader(reader)
        .listener(readerListener)
        .processor(processor)
        .listener(processorListener)
        .writer(writer)
        .listener(writerListener)
        .faultTolerant()
        .skipPolicy(batchProperties.getSkipPolicy().newInstance())
        .taskExecutor(new SimpleAsyncTaskExecutor())
        .throttleLimit(max(DEFAULT_THROTTLE_LIMIT, getRuntime().availableProcessors() - 1))
        .build();
  }

  /**
   * Configure Spring Batch Step that will create skipped requisitions.
   */
  @Bean
  public Step skipPeriodsStep(StepBuilderFactory stepBuilderFactory,
                              FacilityReader reader,
                              FacilityReadListener readerListener,
                              RequisitionWriter writer,
                              RequisitionWriteListener writerListener,
                              SkipPeriodsProcessor processor,
                              SkipPeriodsProcessListener processorListener,
                              ToolProperties toolProperties)
      throws IllegalAccessException, InstantiationException {
    ToolBatchConfiguration batchProperties = toolProperties
        .getConfiguration()
        .getBatch();

    return stepBuilderFactory
        .get("skipPeriodsStep")
        .<String, List<Requisition>>chunk(batchProperties.getChunk())
        .reader(reader)
        .listener(readerListener)
        .processor(processor)
        .listener(processorListener)
        .writer(writer)
        .listener(writerListener)
        .faultTolerant()
        .skipPolicy(batchProperties.getSkipPolicy().newInstance())
        .taskExecutor(new SimpleAsyncTaskExecutor())
        .throttleLimit(max(DEFAULT_THROTTLE_LIMIT, getRuntime().availableProcessors() - 1))
        .build();
  }

  /**
   * Configure Spring Batch Step that will remove duplicates from database.
   */
  @Bean
  public Step removeDuplicatesStep(StepBuilderFactory stepBuilderFactory,
                                   DuplicateReader reader,
                                   DuplicateReadListener readerListener,
                                   RequisitionRemover writer,
                                   RequisitionRemoveListener writerListener,
                                   DuplicateProcessor processor,
                                   DuplicateProcessListener processorListener,
                                   ToolProperties toolProperties)
      throws IllegalAccessException, InstantiationException {
    ToolBatchConfiguration batchProperties = toolProperties
        .getConfiguration()
        .getBatch();

    return stepBuilderFactory
        .get("removeDuplicatesStep")
        .<List<BaseRequisition>, List<BaseRequisition>>chunk(batchProperties.getChunk())
        .reader(reader)
        .listener(readerListener)
        .processor(processor)
        .listener(processorListener)
        .writer(writer)
        .listener(writerListener)
        .faultTolerant()
        .skipPolicy(batchProperties.getSkipPolicy().newInstance())
        .build();
  }

  /**
   * Configure Spring Batch Migration Job.
   */
  @Bean
  public Job migrationJob(JobBuilderFactory jobBuilderFactory, Step migrationStep,
                          Step removeDuplicatesStep, Step skipPeriodsStep,
                          ToolProperties toolProperties) {
    JobBuilder job = jobBuilderFactory
        .get("migrationJob")
        .incrementer(new RunIdIncrementer());

    SimpleJobBuilder builder = new SimpleJobBuilder(job);

    if (toolProperties.getConfiguration().getBatch().isMigration()) {
      builder.next(migrationStep);
    }

    if (toolProperties.getConfiguration().getBatch().isRemoveDuplicates()) {
      builder.next(removeDuplicatesStep);
    }

    if (toolProperties.getConfiguration().getBatch().isSkipPeriods()) {
      builder.next(skipPeriodsStep);
    }
    
    return builder.build();
  }

}
