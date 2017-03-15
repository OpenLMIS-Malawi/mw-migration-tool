package org.openlmis.migration.tool;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

public class App {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

  /**
   * The application start method. If passed arguments are okay it runs with spring context.
   * Otherwise it transform error message and information how to use the application.
   *
   * @param args a list of arguments
   */
  public static void main(String[] args) {
    Arguments arguments = new Arguments();

    JCommander commander = new JCommander();
    commander.setProgramName("SCMgr Migration Tool");
    commander.addObject(arguments);

    try {
      commander.parse(args);

      SpringApplication application = new SpringApplication(AppConfiguration.class);
      application.setBannerMode(Banner.Mode.LOG);
      application.addInitializers(
          cxt -> cxt
              .getBeanFactory()
              .registerSingleton(Arguments.class.getCanonicalName(), arguments)
      );

      application.run();
    } catch (ParameterException exp) {
      LOGGER.error(exp.getMessage());
      commander.usage();
    }
  }

}
