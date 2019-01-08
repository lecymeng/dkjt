package com.sspai.navigationdrawer.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marc on 19.12.2014.
 */
public class Menu {
  public static int SECTION = 0;
  public static int BOTTOM_SECTION = 1;
  public static int DIVISOR = 2;

  private List<Object> sections = new ArrayList<>();
  //private Map<Integer, MaterialSection> sections = new HashMap<Integer, MaterialSection>();

  public List<Object> getSections () {
    return sections;
  }

  public void setSections (List<Object> sections) {
    this.sections = sections;
  }
}
