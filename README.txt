HYDRA2 (HYper-spectral data viewer for Development of Research Applications)


To compile and run, use an IDE, for example NetBeans or Eclipse:

  (1) Java SDK, 7 or later, must be installed.
  
  (2) Add everything under the 'src' directory.

  (3) Add all jar files under lib, including lib/java3d.

  (4) Use src/edu/wisc/ssec/hydra/DataBrowser for the 'main' class to run

  (5) Set this JVM arg: -Djava.ext.dirs=


You may want to use the following options for best performance:

-Dvisad.java3d.imageByRef=true -Dvisad.java3d.textureNpot=true
