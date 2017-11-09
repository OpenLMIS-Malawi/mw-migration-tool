package mw.gov.health.lmis.migration.tool.config;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.OrderNumberConfiguration;

import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;

@Getter
@Setter
public class ToolParameters {
  private LocalDate startDate = LocalDate.now().minusYears(5);
  private LocalDate endDate = LocalDate.now().with(lastDayOfMonth());
  private String creator = "scm";
  private String requestedQuantityExplanation = "transferred from supply manager";
  private TimeZone timeZone = TimeZone.getTimeZone("Africa/Johannesburg");
  private boolean updateExisting = false;
  private OrderNumberConfiguration orderNumberConfiguration = new OrderNumberConfiguration(
      "O", true, false, false
  );
  private List<RequisitionTemplateConfiguration> requisitionTemplates = Lists.newArrayList();

  public void setTimeZone(String timeZone) {
    this.timeZone = TimeZone.getTimeZone(timeZone);
  }

  public void setStartDate(String date) {
    startDate = LocalDate.parse(date);
  }

  public void setEndDate(String date) {
    endDate = LocalDate.parse(date);
  }
}
