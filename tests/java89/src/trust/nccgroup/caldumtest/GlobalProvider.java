package trust.nccgroup.caldumtest;

import trust.nccgroup.caldum.util.TmpLogger;

import java.util.logging.Logger;

import static trust.nccgroup.caldum.annotation.DI.*;

@Provider
public class GlobalProvider {

  @Provide(name = "logger")
  public static Logger globalInjectedLogger = TmpLogger.build("global");

}
