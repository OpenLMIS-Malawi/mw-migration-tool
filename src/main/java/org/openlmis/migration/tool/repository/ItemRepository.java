package org.openlmis.migration.tool.repository;


import org.openlmis.migration.tool.domain.Facility;
import org.openlmis.migration.tool.domain.Item;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemRepository extends ReadOnlyRepository<Item, Integer> {

  @Query(
      "SELECT DISTINCT processingDate " +
          "FROM org.openlmis.migration.tool.domain.Item " +
          "WHERE processingDate IS NOT NULL")
  List<LocalDateTime> getProcessingDates();

  List<Item> findByProcessingDateAndFacility(LocalDateTime processingDate, Facility facility);

}
