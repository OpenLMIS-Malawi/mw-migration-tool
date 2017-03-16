package org.openlmis.migration.tool.openlmis.referencedata.repository;

import com.google.common.collect.Maps;

import org.openlmis.migration.tool.domain.AdjustmentType;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Program;
import org.openlmis.migration.tool.openlmis.referencedata.domain.StockAdjustmentReason;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
public class OpenLmisStockAdjustmentReasonRepository {
  private static final Map<String, StockAdjustmentReason> REASONS = Maps.newConcurrentMap();

  /**
   * Find stock adjustment reason based on arguments.
   */
  public StockAdjustmentReason find(Program programDto, int order,
                                    AdjustmentType adjustmentType) {
    StockAdjustmentReason stockAdjustmentReasonDto = REASONS.get(adjustmentType.getCode());

    if (null == stockAdjustmentReasonDto) {
      stockAdjustmentReasonDto = new StockAdjustmentReason();
      stockAdjustmentReasonDto.setId(UUID.randomUUID());
      stockAdjustmentReasonDto.setProgram(programDto);
      stockAdjustmentReasonDto.setName(adjustmentType.getCode());
      stockAdjustmentReasonDto.setDescription(adjustmentType.getName());
      stockAdjustmentReasonDto.setAdditive(!adjustmentType.getNegative());
      stockAdjustmentReasonDto.setDisplayOrder(order);

      REASONS.put(adjustmentType.getCode(), stockAdjustmentReasonDto);
    }

    return stockAdjustmentReasonDto;
  }

  public Collection<StockAdjustmentReason> findAll() {
    return REASONS.values();
  }

}
