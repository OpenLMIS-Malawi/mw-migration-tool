package org.openlmis.migration.tool.openlmis.referencedata.repository;

import com.google.common.collect.Maps;

import org.openlmis.migration.tool.domain.Item;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Code;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Orderable;
import org.openlmis.migration.tool.openlmis.referencedata.domain.TradeItem;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
public class OpenLmisOrderableRepository {
  private static final Map<String, Orderable> ORDERABLES = Maps.newConcurrentMap();

  public Orderable find(Item item) {
    Orderable orderableDto = ORDERABLES.get(item.getProductName());

    if (null == orderableDto) {
      orderableDto = new TradeItem();
      orderableDto.setId(UUID.randomUUID());
      orderableDto.setProductCode(new Code(item.getProduct().getProductId()));
      orderableDto.setName(item.getProductName());

      ORDERABLES.put(item.getProductName(), orderableDto);
    }

    return orderableDto;
  }

  public Collection<Orderable> findAll() {
    return ORDERABLES.values();
  }

  public Orderable find(String productName) {
    return ORDERABLES.get(productName);
  }
}
