package org.openlmis.migration.tool.openlmis.repository;

import org.openlmis.migration.tool.openlmis.dto.ProgramDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OpenLmisProgramRepository {

  /**
   * Find correct program.
   */
  public ProgramDto find() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(UUID.randomUUID());

    return programDto;
  }

}
