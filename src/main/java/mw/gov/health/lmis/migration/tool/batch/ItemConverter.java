package mw.gov.health.lmis.migration.tool.batch;

import static mw.gov.health.lmis.migration.tool.openlmis.CurrencyConfig.CURRENCY_CODE;
import static mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.LineItemFieldsCalculator.calculateTotalLossesAndAdjustments;
import static mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.OpenLmisNumberUtils.zeroIfNull;
import static mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.PRICE_PER_PACK_IF_NULL;
import static org.apache.commons.lang3.StringUtils.length;

import com.google.common.collect.Lists;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mw.gov.health.lmis.migration.tool.AppContext;
import mw.gov.health.lmis.migration.tool.config.MappingHelper;
import mw.gov.health.lmis.migration.tool.config.ToolProperties;
import mw.gov.health.lmis.migration.tool.openlmis.OnlyId;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.Code;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.Program;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.StockAdjustmentReason;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.repository.OrderableRepository;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.repository.StockAdjustmentReasonRepository;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.StockAdjustment;
import mw.gov.health.lmis.migration.tool.scm.domain.Adjustment;
import mw.gov.health.lmis.migration.tool.scm.domain.AdjustmentType;
import mw.gov.health.lmis.migration.tool.scm.domain.Comment;
import mw.gov.health.lmis.migration.tool.scm.domain.Item;
import mw.gov.health.lmis.migration.tool.scm.repository.AdjustmentTypeAccessRepository;
import mw.gov.health.lmis.migration.tool.scm.service.ItemService;
import mw.gov.health.lmis.migration.tool.scm.service.ProductService;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ItemConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ItemConverter.class);

  @Autowired
  private AdjustmentTypeAccessRepository adjustmentTypeRepository;

  @Autowired
  private StockAdjustmentReasonRepository stockAdjustmentReasonRepository;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private ProductService productService;

  @Autowired
  private ItemService itemService;

  @Autowired
  private ToolProperties toolProperties;

  @Autowired
  private ProductHelper productHelper;

  @Autowired
  private AppContext context;

  /**
   * Converts {@link Item} object into {@link RequisitionLineItem} object.
   */
  public List<RequisitionLineItem> convert(Collection<Item> items, Requisition requisition,
                                           Map<Integer, List<Adjustment>> adjustments,
                                           Map<Integer, List<Comment>> comments) {
    return items
        .parallelStream()
        .map(item -> {
          List<Adjustment> itemAdjustments = adjustments.get(item.getId());
          itemAdjustments = Optional.ofNullable(itemAdjustments).orElse(Lists.newArrayList());

          List<Comment> itemComments = comments.get(item.getId());
          itemComments = Optional.ofNullable(itemComments).orElse(Lists.newArrayList());

          return create(item, requisition, itemAdjustments, itemComments);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(RequisitionLineItem::getOrderableId))
        .entrySet()
        .parallelStream()
        .map(entry -> merge(requisition, entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private RequisitionLineItem merge(Requisition requisition, UUID orderable,
                                    List<RequisitionLineItem> lines) {
    RequisitionLineItem merged = new RequisitionLineItem();
    merged.setStockAdjustments(Lists.newArrayList());
    merged.setPreviousAdjustedConsumptions(Lists.newArrayList());
    merged.setRequisition(requisition);
    merged.setOrderableId(orderable);
    merged.setPricePerPack(
        Money.of(CurrencyUnit.of(CURRENCY_CODE), PRICE_PER_PACK_IF_NULL)
    );
    merged.setSkipped(false);

    merged.setBeginningBalance(sum(lines, RequisitionLineItem::getBeginningBalance));
    merged.setTotalReceivedQuantity(sum(lines, RequisitionLineItem::getTotalReceivedQuantity));
    merged.setTotalConsumedQuantity(sum(lines, RequisitionLineItem::getTotalConsumedQuantity));
    merged.setStockAdjustments(mergeList(lines, RequisitionLineItem::getStockAdjustments));
    merged.setTotalLossesAndAdjustments(
        sum(lines, RequisitionLineItem::getTotalLossesAndAdjustments)
    );
    merged.setTotalStockoutDays(sum(lines, RequisitionLineItem::getTotalStockoutDays));
    merged.setStockOnHand(sum(lines, RequisitionLineItem::getStockOnHand));
    merged.setCalculatedOrderQuantity(sum(lines, RequisitionLineItem::getCalculatedOrderQuantity));
    merged.setRequestedQuantity(sum(lines, RequisitionLineItem::getRequestedQuantity));
    merged.setRequestedQuantityExplanation(
        toolProperties.getParameters().getRequestedQuantityExplanation()
    );
    merged.setAdjustedConsumption(sum(lines, RequisitionLineItem::getAdjustedConsumption));
    merged.setNonFullSupply(false);
    merged.setApprovedQuantity(sum(lines, RequisitionLineItem::getApprovedQuantity));
    merged.setMaxPeriodsOfStock(getMonthsOfStock(merged));

    String remarks = join(lines, RequisitionLineItem::getRemarks);

    if (length(remarks) > 250) {
      LOGGER.warn("The remarks ({}) are too long. Skipping...", remarks);
    } else {
      merged.setRemarks(remarks);
    }

    return merged;
  }

  private int sum(List<RequisitionLineItem> lines,
                  Function<RequisitionLineItem, Integer> field) {
    return lines.stream().map(field).filter(Objects::nonNull).reduce(0, Integer::sum);
  }

  private String join(List<RequisitionLineItem> lines,
                      Function<RequisitionLineItem, String> field) {
    return lines.stream().map(field).filter(Objects::nonNull).collect(Collectors.joining("; "));
  }

  private <T> List<T> mergeList(List<RequisitionLineItem> lines,
                                Function<RequisitionLineItem, List<T>> field) {
    return lines.stream().map(field).flatMap(Collection::stream).collect(Collectors.toList());
  }

  private RequisitionLineItem create(Item item, Requisition requisition,
                                     List<Adjustment> adjustments, List<Comment> comments) {
    Optional<String> productCode = productService.getProductCode(item.getProduct());

    if (!productCode.isPresent()) {
      return null;
    }

    String productCodeValue = productCode.get();

    if (toolProperties.getExclude().getProducts().contains(productCodeValue)) {
      LOGGER.warn("The product code {} is on exclude list. Skipping...", productCodeValue);
      return null;
    }

    Code orderableCode = new Code(productCodeValue);
    OnlyId orderable = orderableRepository.findFirstByProductCode(orderableCode);

    if (null == orderable) {
      LOGGER.error("Can't find orderable with code {}", productCodeValue);
      return null;
    }

    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setStockAdjustments(Lists.newArrayList());
    requisitionLineItem.setPreviousAdjustedConsumptions(Lists.newArrayList());
    requisitionLineItem.setRequisition(requisition);
    requisitionLineItem.setOrderableId(orderable.getId());
    requisitionLineItem.setPricePerPack(
        Money.of(CurrencyUnit.of(CURRENCY_CODE), PRICE_PER_PACK_IF_NULL)
    );

    requisitionLineItem.setSkipped(false);

    Integer closingBalance = productHelper.getClosingBalance(requisition, orderable.getId());
    requisitionLineItem.setBeginningBalance(closingBalance);

    requisitionLineItem.setTotalReceivedQuantity(item.getReceipts());
    requisitionLineItem.setTotalConsumedQuantity(item.getDispensedQuantity());

    List<StockAdjustmentReason> reasons = Lists.newArrayList();

    if (null != adjustments) {
      requisitionLineItem.setStockAdjustments(
          adjustments
              .parallelStream()
              .map(adjustment -> map(adjustment, requisition, reasons))
              .filter(Objects::nonNull)
              .collect(Collectors.toList())
      );
    }

    requisitionLineItem.setTotalLossesAndAdjustments(
        calculateTotalLossesAndAdjustments(requisitionLineItem, reasons)
    );
    requisitionLineItem.setTotalStockoutDays((int) zeroIfNull(item.getStockedOutDays()));
    requisitionLineItem.setStockOnHand(item.getClosingBalance());
    requisitionLineItem.setCalculatedOrderQuantity(item.getCalculatedRequiredQuantity());
    requisitionLineItem.setRequestedQuantity(item.getRequiredQuantity());
    requisitionLineItem.setRequestedQuantityExplanation(
        toolProperties.getParameters().getRequestedQuantityExplanation()
    );
    requisitionLineItem.setAdjustedConsumption(item.getAdjustedDispensedQuantity());
    requisitionLineItem.setNonFullSupply(false);
    requisitionLineItem.setApprovedQuantity(item.getRequiredQuantity());

    requisitionLineItem.setMaxPeriodsOfStock(getMonthsOfStock(requisitionLineItem));

    String remarks = itemService.getNotes(item.getNote(), comments);

    if (length(remarks) > 250) {
      LOGGER.warn("The remarks ({}) are too long. Skipping...", remarks);
    } else {
      requisitionLineItem.setRemarks(remarks);
    }

    return requisitionLineItem;
  }

  private StockAdjustment map(Adjustment adjustment, Requisition requisition,
                              List<StockAdjustmentReason> reasons) {
    AdjustmentType type = adjustmentTypeRepository.findByType(adjustment.getType());
    String name = MappingHelper.getAdjustmentName(toolProperties, type.getName());

    Program program = context.findProgramById(requisition.getProgramId());

    if (null == program) {
      LOGGER.error("Can't find program with id {}", requisition.getProgramId());
      return null;
    }

    StockAdjustmentReason stockAdjustmentReason =
        stockAdjustmentReasonRepository.findByProgramAndName(program, name);

    if (null == stockAdjustmentReason) {
      LOGGER.error(
          "Can't find stock adjustment reason for program {} with name {}",
          program.getCode(), name
      );

      return null;
    }

    StockAdjustment stockAdjustment = new StockAdjustment();
    stockAdjustment.setReasonId(stockAdjustmentReason.getId());
    stockAdjustment.setQuantity(adjustment.getQuantity());

    reasons.add(stockAdjustmentReason);

    return stockAdjustment;
  }

  private BigDecimal getMonthsOfStock(RequisitionLineItem requisitionLineItem) {
    if (0 == zeroIfNull(requisitionLineItem.getAdjustedConsumption())) {
      return BigDecimal.ZERO;
    }

    return BigDecimal.valueOf(requisitionLineItem.getStockOnHand())
        .divide(
            BigDecimal.valueOf(requisitionLineItem.getAdjustedConsumption()),
            1,
            BigDecimal.ROUND_HALF_UP
        );
  }

}
