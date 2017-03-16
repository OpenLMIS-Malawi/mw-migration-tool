package org.openlmis.migration.tool.repository;


import org.openlmis.migration.tool.domain.Facility;
import org.openlmis.migration.tool.domain.Item;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemRepository extends ReadOnlyRepository<Item, Integer> {

  List<Item> findByProcessingDateAndFacility(LocalDateTime processingDate, Facility facility);

}
