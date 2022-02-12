# sleep-irregularity

[Original Sleep Android SRI Feature Repo](https://github.com/petrnalevka/sleep-irregularity/tree/main/app)

**Git clone from this brownhci/sleep-irregularity repo** [https://github.com/brownhci/sleep-irregularity](https://github.com/brownhci/sleep-irregularity)

* **Install [Android Studio IDE](https://developer.android.com/studio)**
  * If on Unix: 
    * `tar -xvf $zipped_filename_of_android_studio`
    * `cd android-studo/bin/`
    * run `./studio.sh` 

* **Gradle and SDK have to be upgraded to match the configurations written in `app > build.gradle`**  
  *  If you face Gradle version is not compatible: 
     1. cd sleep-irregularity 
     2. make a gradle/wrapper dir: `mkdir gradle; cd gradle; mkdir wrapper; cd wrapper;`  
     3. create `gradle-wrapper.properties` file 
     4. Create gradle-wrapper.properties, giving the correct distributionUrl or gradle build version with following information: 
    
     `distributionBase=GRADLE_USER_HOME`

     `distributionPath=wrapper/dists`

     `distributionUrl=https\://services.gradle.org/distributions/gradle-7.0.2-bin.zip`

     `zipStoreBase=GRADLE_USER_HOME`

     `zipStorePath=wrapper/dists`

![gradle distribution synchronization](https://github.com/brownhci/SleepRegularity/blob/main/wiki_imgs/gradle-distribution-sync.png)
   

 
* **Configure the gradle project to point to ~/sleep-irregularity/app** 

![Edit config part 1](https://github.com/brownhci/SleepRegularity/blob/main/wiki_imgs/edit_config_1.png)
![Edit config part 2](https://github.com/brownhci/SleepRegularity/blob/main/wiki_imgs/edit_config_2.png)

* **To run java files find a java folder `i.e. app/src/main/java` hover, right click, click "Run All Tests"**
  * If the option to "Run All Test" doesn't show up, try syncing gradle files: 
  `Files > Sync Project with Gradle Files`
![Sync Gradle Files](https://github.com/brownhci/SleepRegularity/blob/main/wiki_imgs/sync_gradle_files.png)
