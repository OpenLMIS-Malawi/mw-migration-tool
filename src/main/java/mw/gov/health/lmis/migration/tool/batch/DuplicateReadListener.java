package mw.gov.health.lmis.migration.tool.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.stereotype.Component;

import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;

import java.util.List;

@Component
public class DuplicateReadListener implements ItemReadListener<List<Requisition>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateReadListener.class);

  @Override
  public void beforeRead() {
    LOGGER.debug("Read duplicates");
  }

  @Override
  public void afterRead(List<Requisition> item) {
    LOGGER.info("Read {} duplicates", item.size());
  }

  @Override
  public void onReadError(Exception exp) {
    LOGGER.error("Cannot read duplicates", exp);
  }

}
