package mw.gov.health.lmis.migration.tool.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;

import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;

import java.util.Collection;
import java.util.List;

public class OlmisLoadListener
    implements ItemWriteListener<List<Requisition>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(OlmisLoadListener.class);

  @Override
  public void beforeWrite(List<? extends List<Requisition>> items) {
    LOGGER.debug(
        "Save {} requisitions to database",
        items.stream().mapToLong(Collection::size).sum()
    );
  }

  @Override
  public void afterWrite(List<? extends List<Requisition>> items) {
    LOGGER.info(
        "Saved {} requisitions to database",
        items.stream().mapToLong(Collection::size).sum()
    );
  }

  @Override
  public void onWriteError(Exception exp, List<? extends List<Requisition>> items) {
    LOGGER.error(
        "Cannot save {} requisitions to database",
        items.stream().mapToLong(Collection::size).sum(),
        exp
    );
  }

}
