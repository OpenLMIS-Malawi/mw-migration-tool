package mw.gov.health.lmis.migration.tool;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

import de.vandermeer.asciitable.AT_Context;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciithemes.TA_Grid;
import de.vandermeer.asciithemes.TA_GridThemes;
import de.vandermeer.asciithemes.u8.U8_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.ProcessingPeriod;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.Program;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.repository.RequisitionRepository;
import mw.gov.health.lmis.migration.tool.scm.domain.Item;
import mw.gov.health.lmis.migration.tool.scm.domain.Main;
import mw.gov.health.lmis.migration.tool.scm.repository.MainAccessRepository;
import mw.gov.health.lmis.migration.tool.scm.service.ItemService;
import mw.gov.health.lmis.migration.tool.scm.service.MainService;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReportService.class);

  @Autowired
  private MainAccessRepository mainRepository;

  @Autowired
  private MainService mainService;

  @Autowired
  private ItemService itemService;

  @Autowired
  private AppContext context;

  @Autowired
  private RequisitionRepository requisitionRepository;

  void printSummaryReport() {
    LOGGER.info("Create SCMgr Summary report");
    Table<UUID, Integer, Integer> scmSummary = getScmSummary();

    LOGGER.info("Create OpenLMIS Summary report");
    Set<Integer> years = scmSummary.columnKeySet();
    List<ProcessingPeriod> periods = context.findPeriods(period -> {
      int startYear = period.getStartDate().getYear();
      return years.contains(startYear);
    });

    Table<UUID, Integer, Integer> olmisSummary = getOlmisSummary(periods);

    AsciiTable report = generate(scmSummary, olmisSummary);

    LOGGER.info("The final summary report\n{}", report.render());
  }

  private AsciiTable generate(Table<UUID, Integer, Integer> scmSummary,
                              Table<UUID, Integer, Integer> olmisSummary) {
    AsciiTable report = new AsciiTable();

    report.addRule();
    report.addRow("Year", "Program", "OpenLMIS", "SCMgr");
    report.addRule();

    for (Cell<UUID, Integer, Integer> cell : scmSummary.cellSet()) {
      Integer year = cell.getColumnKey();
      UUID programId = cell.getRowKey();
      String name = context.findProgramById(programId).getName();

      Integer scmCount = cell.getValue();
      scmCount = Optional.ofNullable(scmCount).orElse(0);

      Integer olmisCount = olmisSummary.get(programId, year);
      olmisCount = Optional.ofNullable(olmisCount).orElse(0);

      NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);

      report.addRow(year, name, format.format(olmisCount), format.format(scmCount));
    }

    report.addRule();
    report.setTextAlignment(TextAlignment.CENTER);

    return report;
  }

  private Table<UUID, Integer, Integer> getScmSummary() {
    List<Main> mains = mainRepository.searchInPeriod();

    List<SingleSummary> summaries = mains
        .parallelStream()
        .map(this::convert)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    return grouping(summaries);
  }

  private Table<UUID, Integer, Integer> getOlmisSummary(List<ProcessingPeriod> periods) {
    Set<UUID> periodIds = periods
        .stream()
        .map(ProcessingPeriod::getId)
        .collect(Collectors.toSet());

    List<SingleSummary> summaries = requisitionRepository
        .countByProgramAndPeriod(periodIds)
        .parallelStream()
        .map(this::convert)
        .collect(Collectors.toList());

    return grouping(summaries);
  }

  private List<SingleSummary> convert(Main main) {
    List<Item> items = itemService.search(main.getProcessingDate(), main.getFacility());
    Map<String, Collection<Item>> map = itemService.groupByCategory(items);

    int year = mainService.getProcessingDate(main).getYear();
    List<SingleSummary> summaries = Lists.newArrayList();

    for (Map.Entry<String, Collection<Item>> entry : map.entrySet()) {
      Program program = context.findProgramByCode(entry.getKey());

      summaries.add(new SingleSummary(program.getId(), year, entry.getValue().size()));
    }

    return summaries;
  }

  private SingleSummary convert(Object[] row) {
    int year = context.findPeriodById((UUID) row[1]).getStartDate().getYear();
    Long count = (Long) row[2];

    return new SingleSummary((UUID) row[0], year, count.intValue());
  }

  private Table<UUID, Integer, Integer> grouping(List<SingleSummary> summaries) {
    Table<UUID, Integer, Integer> table = TreeBasedTable.create();

    for (SingleSummary summary : summaries) {
      Integer existing = table.get(summary.program, summary.year);
      existing = Optional.ofNullable(existing).orElse(0);

      table.put(summary.program, summary.year, existing + summary.lines);
    }

    return table;
  }

  @AllArgsConstructor
  private static final class SingleSummary {
    private final UUID program;
    private final int year;
    private final int lines;
  }

}
