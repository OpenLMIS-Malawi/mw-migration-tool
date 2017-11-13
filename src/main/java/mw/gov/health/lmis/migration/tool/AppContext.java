package mw.gov.health.lmis.migration.tool;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.Getter;
import mw.gov.health.lmis.migration.tool.config.ToolProperties;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.ProcessingPeriod;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.ProcessingSchedule;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.Program;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.User;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.repository.ProcessingPeriodRepository;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.repository.ProgramRepository;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.repository.UserRepository;
import mw.gov.health.lmis.migration.tool.scm.repository.PreparedAccessRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public final class AppContext implements InitializingBean {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppContext.class);

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private ProcessingPeriodRepository periodRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ToolProperties toolProperties;

  @Autowired
  private ApplicationContext context;

  @Getter
  private List<Program> programs;

  private List<ProcessingPeriod> allPeriods;

  @Getter
  private List<ProcessingPeriod> periods;

  @Getter
  private User user;

  @Override
  public void afterPropertiesSet() {
    LOGGER.info("Initialize batch context...");
    TimeZone.setDefault(toolProperties.getParameters().getTimeZone());

    programs = Lists.newArrayList(programRepository.findAll());
    LOGGER.info("Found {} programs", programs.size());

    allPeriods = periodRepository.findAll();
    LOGGER.info("Found {} periods", allPeriods.size());

    LocalDate startDate = toolProperties.getParameters().getStartDate();
    LocalDate endDate = toolProperties.getParameters().getEndDate();
    periods = allPeriods
        .stream()
        .filter(period ->
            !period.getStartDate().isBefore(startDate) && !period.getEndDate().isAfter(endDate))
        .collect(Collectors.toList());
    LOGGER.info("Select {} periods between {} and {}", periods.size(), startDate, endDate);

    String username = toolProperties.getParameters().getCreator();
    user = userRepository.findByUsername(username);

    if (null == user) {
      throw new IllegalStateException("Can't find user with username: " + username);
    } else {
      LOGGER.info("Found user with username: {}", username);
    }

    context
        .getBeansOfType(PreparedAccessRepository.class)
        .values()
        .forEach(PreparedAccessRepository::init);

    LOGGER.info("Initialized batch context...");
  }

  public Program findProgramById(UUID id) {
    return findProgram(elem -> id.equals(elem.getId()));
  }

  public Program findProgramByCode(String code) {
    return findProgram(elem -> code.equals(elem.getCode().toString()));
  }

  private Program findProgram(Predicate<Program> predicate) {
    return programs.stream().filter(predicate).findFirst().orElse(null);
  }

  public ProcessingPeriod findPeriod(Predicate<ProcessingPeriod> predicate) {
    return periods.stream().filter(predicate).findFirst().orElse(null);
  }

  List<ProcessingPeriod> findPeriods(Predicate<ProcessingPeriod> predicate) {
    return periods.stream().filter(predicate).collect(Collectors.toList());
  }

  /**
   * Find previous period based on current.
   */
  public ProcessingPeriod findPreviousPeriod(UUID periodId) {
    ProcessingPeriod period = findPeriodById(periodId);

    if (null == period) {
      return null;
    }

    List<ProcessingPeriod> collection = searchPeriods(
        period.getProcessingSchedule(), period.getStartDate()
    );

    if (null == collection || collection.isEmpty()) {
      return null;
    }

    // create a list...
    List<ProcessingPeriod> list = new ArrayList<>(collection);
    // ...remove the latest period from the list...
    list.removeIf(p -> p.getId().equals(periodId));
    // .. and sort elements by startDate property DESC.
    list.sort((one, two) -> ObjectUtils.compare(two.getStartDate(), one.getStartDate()));

    return list.isEmpty() ? null : list.get(0);
  }

  public ProcessingPeriod findPeriodById(UUID id) {
    return periods.stream().filter(elem -> id.equals(elem.getId())).findFirst().orElse(null);
  }

  private List<ProcessingPeriod> searchPeriods(ProcessingSchedule schedule, LocalDate startDate) {
    return allPeriods
        .stream()
        .filter(period ->
            Objects.equals(schedule, period.getProcessingSchedule())
                && !period.getStartDate().isAfter(startDate))
        .collect(Collectors.toList());
  }

}
