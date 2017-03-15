package org.openlmis.migration.tool;

import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;

import net.ucanaccess.jdbc.UcanaccessDriver;

import org.hibernate.dialect.SQLServerDialect;
import org.openlmis.migration.tool.service.TransformService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;

import javax.sql.DataSource;

@SpringBootApplication
@EnableTransactionManagement
public class AppConfiguration {

  /**
   * Here the application starts with spring context.
   */
  @Bean
  public CommandLineRunner commandLineRunner(TransformService transformService) {
    return (args) -> transformService.transform();
  }

  /**
   * DataSource definition for database connection.
   */
  @Bean
  public DataSource dataSource(Arguments arguments) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(UcanaccessDriver.class.getName());
    dataSource.setUrl("jdbc:ucanaccess://" + arguments.getFile() + ";memory=false");

    return dataSource;
  }

  /**
   * Declare the JPA entity manager factory.
   */
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(Arguments arguments) {
    LocalContainerEntityManagerFactoryBean entityManagerFactory =
        new LocalContainerEntityManagerFactoryBean();

    entityManagerFactory.setDataSource(dataSource(arguments));
    entityManagerFactory.setPackagesToScan("org.openlmis.migration.tool.domain");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    entityManagerFactory.setJpaVendorAdapter(vendorAdapter);

    Properties properties = new Properties();
    properties.setProperty(DIALECT, SQLServerDialect.class.getName());
    properties.setProperty(SHOW_SQL, "false");
    properties.setProperty(HBM2DDL_AUTO, "validate");

    entityManagerFactory.setJpaProperties(properties);

    return entityManagerFactory;
  }

  /**
   * Declare the transaction manager.
   */
  @Bean
  public JpaTransactionManager transactionManager(Arguments arguments) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(entityManagerFactory(arguments).getObject());

    return transactionManager;
  }

  /**
   * PersistenceExceptionTranslationPostProcessor is a bean post processor which adds an advisor
   * to any bean annotated with Repository so that any platform-specific exceptions are caught
   * and then rethrown as one Spring's unchecked data access exceptions (i.e. a subclass of
   * DataAccessException).
   */
  @Bean
  public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
    return new PersistenceExceptionTranslationPostProcessor();
  }

}
