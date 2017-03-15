package org.openlmis.migration.tool.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TransformService {

  @Autowired
  private ItemTransformService itemTransformService;

  public void transform() {
    itemTransformService.transform();

    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
