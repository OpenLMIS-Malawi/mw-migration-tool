package mw.gov.health.lmis.migration.tool.batch;

import static mw.gov.health.lmis.migration.tool.openlmis.ExternalStatus.APPROVED;
import static mw.gov.health.lmis.migration.tool.openlmis.ExternalStatus.AUTHORIZED;
import static mw.gov.health.lmis.migration.tool.openlmis.ExternalStatus.INITIATED;
import static mw.gov.health.lmis.migration.tool.openlmis.ExternalStatus.SUBMITTED;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import mw.gov.health.lmis.migration.tool.config.MappingHelper;
import mw.gov.health.lmis.migration.tool.config.ToolProperties;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.Facility;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.ProcessingPeriod;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.Program;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.RequisitionGroup;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.User;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.repository.FacilityRepository;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.repository.RequisitionGroupProgramScheduleRepository;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.RequisitionTemplate;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.StatusChange;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.repository.RequisitionRepository;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.repository.RequisitionTemplateRepository;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.service.RequisitionService;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.util.RequisitionUtil;
import mw.gov.health.lmis.migration.tool.scm.domain.Adjustment;
import mw.gov.health.lmis.migration.tool.scm.domain.Comment;
import mw.gov.health.lmis.migration.tool.scm.domain.Item;
import mw.gov.health.lmis.migration.tool.scm.domain.Main;
import mw.gov.health.lmis.migration.tool.scm.repository.AdjustmentAccessRepository;
import mw.gov.health.lmis.migration.tool.scm.repository.CommentAccessRepository;
import mw.gov.health.lmis.migration.tool.scm.service.ItemService;
import mw.gov.health.lmis.migration.tool.scm.service.MainService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MigrationProcessor implements ItemProcessor<Main, List<Requisition>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationProcessor.class);

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private RequisitionTemplateRepository requisitionTemplateRepository;

  @Autowired
  private RequisitionGroupProgramScheduleRepository
      requisitionGroupProgramScheduleRepository;

  @Autowired
  private ItemConverter itemConverter;

  @Autowired
  private ItemService itemService;

  @Autowired
  private MainService mainService;

  @Autowired
  private RequisitionService requisitionService;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private AdjustmentAccessRepository adjustmentRepository;

  @Autowired
  private CommentAccessRepository commentRepository;

  @Autowired
  private ToolProperties toolProperties;

  @Autowired
  private ProductHelper productHelper;

  @Autowired
  private AppBatchContext context;

  /**
   * Converts the given {@link Main} object into {@link Requisition} object.
   */
  @Override
  public List<Requisition> process(Main item) {
    LocalDate processingDate = mainService.getProcessingDate(item);

    if (null == processingDate) {
      LOGGER.error("Can't convert processing date to LocalDate instance");
      return Lists.newArrayList();
    }

    List<Item> items = itemService.search(item.getProcessingDate(), item.getFacility());

    if (isEmpty(items)) {
      LOGGER.warn(
          "No items for processing date: {} and facility: {}",
          processingDate, item.getFacility()
      );
      return Lists.newArrayList();
    }

    List<Integer> ids = items.stream().map(Item::getId).collect(Collectors.toList());
    Map<Integer, List<Adjustment>> adjustments = adjustmentRepository.search(ids);
    Map<Integer, List<Comment>> comments = commentRepository.search(ids);

    String code = MappingHelper.getFacilityCode(toolProperties, item.getFacility());
    Facility facility = facilityRepository.findByCode(code);

    if (null == facility) {
      LOGGER.error("Can't find facility with code {}", code);
      return Lists.newArrayList();
    }

    ProcessingPeriod period = context.findPeriod(
        elem -> !elem.getStartDate().isAfter(processingDate)
            && !elem.getEndDate().isBefore(processingDate)
    );

    if (null == period) {
      LOGGER.error("Can't find period for processing date {}", processingDate);
      return Lists.newArrayList();
    }

    return itemService
        .groupByCategory(items)
        .entrySet()
        .parallelStream()
        .map(entry -> create(entry, item, facility, period, adjustments, comments))
        .filter(this::isCorrect)
        .collect(Collectors.toList());
  }

  private boolean isCorrect(Requisition requisition) {
    if (null == requisition) {
      return false;
    }

    boolean isEmpty = RequisitionUtil.isEmpty(requisition);

    if (isEmpty) {
      LOGGER.warn(
          "Created empty requisition (all lines have zero for all columns). Skipping..."
      );
    }

    return !isEmpty;
  }

  private Requisition create(Map.Entry<String, Collection<Item>> entry, Main main,
                             Facility facility, ProcessingPeriod period,
                             Map<Integer, List<Adjustment>> adjustmens,
                             Map<Integer, List<Comment>> comments) {
    Program program = context.findProgramByCode(entry.getKey());

    if (null == program) {
      LOGGER.error("Can't find program with code {}", entry.getKey());
      return null;
    }

    if (!CollectionUtils.isEmpty(toolProperties.getExclude().getForms())) {
      boolean exclude = toolProperties
          .getExclude()
          .getForms()
          .stream()
          .anyMatch(
              form -> form.match(facility.getCode(), period.getName(), program.getCodeValue())
          );

      if (exclude) {
        LOGGER.warn(
            "Requisition for facility {}, program {} and period {} is on exclude list. Skipping...",
            facility.getCode(), program.getCode(), period.getName()
        );
        return null;
      }
    }

    boolean exists = requisitionRepository
        .existsByFacilityIdAndProgramIdAndProcessingPeriodId(
            facility.getId(), program.getId(), period.getId()
        );

    Requisition requisition;

    if (exists) {
      if (toolProperties.getParameters().isUpdateExisting()) {
        requisition = requisitionRepository.findByFacilityIdAndProgramIdAndProcessingPeriodId(
            facility.getId(), program.getId(), period.getId()
        ).get(0);
      } else {
        LOGGER.warn(
            "Requisition for facility {}, program {} and period {} exists. Skipping...",
            facility.getCode(), program.getCode(), period.getName()
        );
        return null;
      }
    } else {
      requisition = new Requisition();
    }

    requisition.setFacilityId(facility.getId());
    requisition.setProgramId(program.getId());
    requisition.setProcessingPeriodId(period.getId());
    requisition.setEmergency(false);
    requisition.setNumberOfMonthsInPeriod(period.getDurationInMonths());

    if (null == requisition.getId()) {
      RequisitionTemplate template = requisitionTemplateRepository
          .findFirstByProgramIdOrderByCreatedDateDesc(program.getId());

      requisition.setTemplate(template);
    }

    requisition.setPreviousRequisitions(Lists.newArrayList());
    requisition.setAvailableNonFullSupplyProducts(Sets.newHashSet());
    requisition.setCreatedDate(convert(main.getModifiedDate(), period.getStartDate()));
    requisition.setModifiedDate(convert(main.getModifiedDate(), period.getEndDate()));
    requisition.setStatus(APPROVED);

    Collection<Item> items = entry.getValue().stream().distinct().collect(Collectors.toList());
    List<RequisitionLineItem> lineItems = itemConverter
        .convert(items, requisition, adjustmens, comments);

    if (null == requisition.getRequisitionLineItems()) {
      requisition.setRequisitionLineItems(Lists.newArrayList());
    }

    requisition.getRequisitionLineItems().clear();
    requisition.getRequisitionLineItems().addAll(lineItems);

    productHelper.add(requisition);

    List<RequisitionGroupProgramSchedule> schedule = requisitionGroupProgramScheduleRepository
        .findByProgramAndFacility(program.getId(), facility.getId());

    if (!schedule.isEmpty()) {
      RequisitionGroup requisitionGroup = schedule.get(0).getRequisitionGroup();
      requisition.setSupervisoryNodeId(requisitionGroup.getSupervisoryNode().getId());
    } else {
      LOGGER.warn(
          "Can't set supervisory node ID for program {} and facility {}",
          program.getCode(), facility.getCode()
      );
    }

    User author = context.getUser();
    UUID authorId = author.getId();

    requisition.getStatusChanges()
        .add(StatusChange.newStatusChange(requisition, authorId, INITIATED));
    requisition.getStatusChanges()
        .add(StatusChange.newStatusChange(requisition, authorId, SUBMITTED));
    requisition.getStatusChanges()
        .add(StatusChange.newStatusChange(requisition, authorId, AUTHORIZED));
    requisition.getStatusChanges()
        .add(StatusChange.newStatusChange(requisition, authorId, APPROVED));

    requisitionService.addStatusMessage(requisition, author, main.getNotes());

    requisitionService.convertToOrder(requisition, author, program, facility);

    return requisition;
  }

  private ZonedDateTime convert(Date date, LocalDate localDate) {
    ZoneId zoneId = toolProperties.getParameters().getTimeZone().toZoneId();

    if (null != date) {
      return date.toInstant().atZone(zoneId);
    }

    if (null != localDate) {
      return localDate.atStartOfDay(zoneId);
    }

    return null;
  }

}
