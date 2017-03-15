package org.openlmis.migration.tool.openlmis.repository;

import org.openlmis.migration.tool.openlmis.domain.RequisitionTemplate;
import org.openlmis.migration.tool.openlmis.dto.ProgramDto;
import org.springframework.stereotype.Service;

@Service
public class OpenLmisRequisitionTemplateRepository {

  public RequisitionTemplate find(ProgramDto program) {
    RequisitionTemplate template = new RequisitionTemplate();
    template.setProgramId(program.getId());

    // TODO: add columns to template

    return template;
  }

}
