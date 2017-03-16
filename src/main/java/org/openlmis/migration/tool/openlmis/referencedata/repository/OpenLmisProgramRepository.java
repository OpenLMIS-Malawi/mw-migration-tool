package org.openlmis.migration.tool.openlmis.referencedata.repository;

import org.openlmis.migration.tool.openlmis.referencedata.domain.Program;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OpenLmisProgramRepository {

  /**
   * Find correct program.
   */
  public Program find() {
    Program program = new Program();
    program.setId(UUID.randomUUID());

    return program;
  }

}
