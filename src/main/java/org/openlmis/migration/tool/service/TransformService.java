package org.openlmis.migration.tool.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransformService {

  @Autowired
  private ItemTransformService itemTransformService;

  public void transform() {
    itemTransformService.transform();
  }

}
