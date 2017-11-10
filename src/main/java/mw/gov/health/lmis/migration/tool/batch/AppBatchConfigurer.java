package mw.gov.health.lmis.migration.tool.batch;

import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
public class AppBatchConfigurer implements BatchConfigurer, InitializingBean {
  private JobRepository jobRepository;
  private JobLauncher jobLauncher;
  private JobExplorer jobExplorer;

  @Setter
  private PlatformTransactionManager transactionManager;

  @Override
  public void afterPropertiesSet() {
    try {
      MapJobRepositoryFactoryBean jobRepositoryFactory = new MapJobRepositoryFactoryBean(
          new ResourcelessTransactionManager()
      );
      jobRepositoryFactory.afterPropertiesSet();
      jobRepository = jobRepositoryFactory.getObject();

      MapJobExplorerFactoryBean jobExplorerFactory = new MapJobExplorerFactoryBean(
          jobRepositoryFactory
      );
      jobExplorerFactory.afterPropertiesSet();
      jobExplorer = jobExplorerFactory.getObject();

      SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
      jobLauncher.setJobRepository(jobRepository);
      jobLauncher.afterPropertiesSet();

      this.jobLauncher = jobLauncher;
    } catch (Exception e) {
      throw new BatchConfigurationException(e);
    }
  }
}
