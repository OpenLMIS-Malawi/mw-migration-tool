package mw.gov.health.lmis.migration.tool.config;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class ToolConfiguration {
  private File accessFile;
  private String password;
  private ToolOlmisConfiguration olmis = new ToolOlmisConfiguration();
  private ToolBatchConfiguration batch = new ToolBatchConfiguration();
}
