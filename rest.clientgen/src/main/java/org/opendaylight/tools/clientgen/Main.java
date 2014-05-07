package org.opendaylight.tools.clientgen;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class Main {

    private static final String HELPER_LOCATION =
        System.getProperty("systest.generator.helper.location");
    private static final String NB_MODULE_NAME =
        System.getProperty("systest.generator.nb.module");
  private static final String PKG =
      System.getProperty("systest.generator.package",
          "org.opendaylight.tools.client.rest") + ".";


    public static void main(String[] args) {
      if(HELPER_LOCATION == null ) {
        throw new IllegalArgumentException(
            "Location for generating helper classes is not provided");
      }
      File file = new File(HELPER_LOCATION);
      if (!file.exists()) {
        file.mkdirs();
      }
      if (!file.isDirectory()) {
        throw new IllegalArgumentException("Not a directory: " + file);
      }
      System.out.println("Building Helper Classes ... ");
      // check if only a module needs generation
      if (NB_MODULE_NAME != null && ! NB_MODULE_NAME.isEmpty()) {
        String[] parts = NB_MODULE_NAME.split(",");
        Set<String> nbs = new HashSet<String>();
        for (String x : parts) {
          nbs.add(x.trim());
        }
        CodeGenUtil.generateHelperClasses(file, nbs, PKG);
      } else {
        CodeGenUtil.generateHelperClasses(file, PKG);
      }
      System.out.println("Successfully generated helper classes in: " + HELPER_LOCATION);
    }

}
