package org.openlmis.migration.tool.openlmis.referencedata.repository;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import org.openlmis.migration.tool.domain.SystemDefault;
import org.openlmis.migration.tool.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.migration.tool.repository.SystemDefaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OpenLmisProcessingPeriodRepository {

  @Autowired
  private SystemDefaultRepository systemDefaultRepository;

  /**
   * Retrieve correct processing period based on passed datetime.
   */
  public ProcessingPeriod find(LocalDateTime processingDateTime) {
    SystemDefault systemDefault = systemDefaultRepository
        .findAll()
        .iterator()
        .next();

    LocalDate processingDate = processingDateTime.toLocalDate();
    LocalDate startDate = processingDate.with(firstDayOfMonth());

    long numberOfMonths = systemDefault.getReportingPeriod() - 1L;

    LocalDate endDate = processingDate.plusMonths(numberOfMonths).with(lastDayOfMonth());

    ProcessingPeriod processingPeriod = new ProcessingPeriod();
    processingPeriod.setId(UUID.randomUUID());
    processingPeriod.setStartDate(startDate);
    processingPeriod.setEndDate(endDate);

    return processingPeriod;
  }

}
