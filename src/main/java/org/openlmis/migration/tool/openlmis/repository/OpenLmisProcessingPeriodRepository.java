package org.openlmis.migration.tool.openlmis.repository;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import org.openlmis.migration.tool.domain.SystemDefault;
import org.openlmis.migration.tool.openlmis.dto.ProcessingPeriodDto;
import org.openlmis.migration.tool.repository.SystemDefaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Service
public class OpenLmisProcessingPeriodRepository {

  @Autowired
  private SystemDefaultRepository systemDefaultRepository;

  public ProcessingPeriodDto find(LocalDateTime processingDateTime) {
    SystemDefault systemDefault = systemDefaultRepository
        .findAll()
        .iterator()
        .next();

    LocalDate processingDate = processingDateTime.toLocalDate();
    LocalDate startDate = processingDate.with(firstDayOfMonth());

    long numberOfMonths = systemDefault.getReportingPeriod() - 1L;

    LocalDate endDate = processingDate.plusMonths(numberOfMonths).with(lastDayOfMonth());

    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setId(UUID.randomUUID());
    processingPeriodDto.setStartDate(startDate);
    processingPeriodDto.setEndDate(endDate);
    processingPeriodDto.setDurationInMonths(Period.between(startDate, endDate).getMonths() + 1);

    return processingPeriodDto;
  }

}
