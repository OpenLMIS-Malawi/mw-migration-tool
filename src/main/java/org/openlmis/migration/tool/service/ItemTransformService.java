package org.openlmis.migration.tool.service;

import org.openlmis.migration.tool.domain.Adjustment;
import org.openlmis.migration.tool.domain.Purpose;
import org.openlmis.migration.tool.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class ItemTransformService {

  @Autowired
  private ItemRepository itemRepository;

  public void transform() {
    System.out.printf(
        "%-4s|%-14s|%-17s|%-13s|%-57s|%-14s|%-18s|%-16s|%-18s|%-17s|%-17s|%-21s|%-18s|%-20s|%-17s|%-9s|%-12s%n",
        "ID", "Facility Code", "Period", "Product Code", "Product Description", "Stock on Hand",
        "Adjustment Amount", "Adjustment Type", "Quantity Consumed", "Purpose of Use",
        "Stocked Out Days", "Adjusted Consumption", "Months of Stock", "Calculated Quantity",
        "Reorder Quantity", "Receipts", "Stocked Out?"
    );

    StreamSupport
        .stream(itemRepository.findAll().spliterator(), false)
        .filter(item -> "A".equalsIgnoreCase(item.getFacility().getCode()))
        .sorted((o1, o2) -> {
          int compareTo = o1.getProcessingDate().compareTo(o2.getProcessingDate());

          if (0 != compareTo) {
            return compareTo;
          }

          return Integer.compare(
              o1.getCategoryProduct().getOrder(),
              o2.getCategoryProduct().getOrder()
          );
        })
        .forEach(item -> {
          System.out.printf(
              "%-4s|%-14s|%-17s|%-13s|%-57s|%-14s|%-18s|%-16s|%-18s|%-17s|%-17s|%-21s|%-18s|%-20s|%-17s|%-9s|%-12s%n",
              item.getId(), item.getFacility().getCode(), item.getProcessingDate(),
              item.getProduct(), item.getProductName(), item.getClosingBalance(),
              countAdjustments(item.getAdjustments()), item.getAdjustmentType(),
              item.getDispensedQuantity(), countPurposes(item.getPurposes()),
              item.getStockedOutDays(), item.getAdjustedDispensedQuantity(),
              "<months_of_stock>", item.getCalculatedRequiredQuantity(), item.getRequiredQuantity(),
              item.getReceipts(), item.getProductStockedOut()
          );
        });
  }

  private int countPurposes(List<Purpose> purposes) {
    return null != purposes
        ? purposes.stream().map(Purpose::getQuantity).reduce(0, (a, b) -> a + b)
        : 0;
  }

  private int countAdjustments(List<Adjustment> adjustments) {
    return null != adjustments
        ? adjustments.stream().map(Adjustment::getQuantity).reduce(0, (a, b) -> a + b)
        : 0;
  }

}
