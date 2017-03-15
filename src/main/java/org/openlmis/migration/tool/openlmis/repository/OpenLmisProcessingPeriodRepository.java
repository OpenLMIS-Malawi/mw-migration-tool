package org.openlmis.migration.tool.openlmis.repository;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import org.openlmis.migration.tool.openlmis.dto.ProcessingPeriodDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Service
public class OpenLmisProcessingPeriodRepository {

  public ProcessingPeriodDto find(LocalDateTime processingDateTime) {
    LocalDate processingDate = processingDateTime.toLocalDate();
    LocalDate startDate = processingDate.with(firstDayOfMonth());
    LocalDate endDate = processingDate.plusMonths(2).with(lastDayOfMonth());

    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setId(UUID.randomUUID());
    processingPeriodDto.setStartDate(startDate);
    processingPeriodDto.setEndDate(endDate);
    processingPeriodDto.setDurationInMonths(Period.between(startDate, endDate).getMonths() + 1);

    return processingPeriodDto;
  }

}
