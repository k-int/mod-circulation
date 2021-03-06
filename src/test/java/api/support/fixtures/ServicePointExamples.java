package api.support.fixtures;

import api.support.builders.ServicePointBuilder;

public class ServicePointExamples {
  
  public static ServicePointBuilder basedUponCircDesk1() {
    return new ServicePointBuilder("Circ Desk 1", "cd1",
        "Circulation Desk -- Hallway").withPickupLocation(Boolean.TRUE);
  }
  
  public static ServicePointBuilder basedUponCircDesk2() {
    return new ServicePointBuilder("Circ Desk 2", "cd2",
        "Circulation Desk -- Back Entrance").withPickupLocation(Boolean.TRUE);
  }
  
  public static ServicePointBuilder basedUponCircDesk3() {
    return new ServicePointBuilder("Circ Desk 3", "cd3",
        "Circulation Desk -- Dumpster").withPickupLocation(Boolean.FALSE);
  }
  
}
