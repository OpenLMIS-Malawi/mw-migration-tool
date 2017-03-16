package org.openlmis.migration.tool.openlmis.referencedata.repository;

import org.openlmis.migration.tool.openlmis.referencedata.domain.Facility;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OpenLmisFacilityRepository {

  /**
   * Find correct OpenLMIS facility based on data from SCMgr facility object.
   */
  public Facility find(org.openlmis.migration.tool.domain.Facility facility) {
    Facility openLmisFacility = new Facility();
    openLmisFacility.setId(UUID.randomUUID());
    openLmisFacility.setName(facility.getName());
    openLmisFacility.setCode(facility.getCode());

    return openLmisFacility;
  }

}
