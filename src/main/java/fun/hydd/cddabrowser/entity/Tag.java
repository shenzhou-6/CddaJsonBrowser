package fun.hydd.cddabrowser.entity;

import java.util.Date;

public class Tag {
  private String name;
  private String message;
  private Date date;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @Override
  public String toString() {
    return "Tag{" +
      "name='" + name + '\'' +
      ", message='" + message + '\'' +
      ", date=" + date +
      '}';
  }
}
