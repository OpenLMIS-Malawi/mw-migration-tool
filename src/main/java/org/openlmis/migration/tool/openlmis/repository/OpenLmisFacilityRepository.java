package org.openlmis.migration.tool.openlmis.repository;

import org.openlmis.migration.tool.domain.Facility;
import org.openlmis.migration.tool.openlmis.dto.FacilityDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OpenLmisFacilityRepository {

  public FacilityDto find(Facility facility) {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(UUID.randomUUID());
    facilityDto.setName(facility.getName());
    facilityDto.setCode(facility.getCode());

    return facilityDto;
  }

}
