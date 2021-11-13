package fun.hydd.cddabrowser.exception;

public class NoNeedUpdateException extends Exception {
  @Override
  public String getMessage() {
    return "Version no need update!";
  }
}
