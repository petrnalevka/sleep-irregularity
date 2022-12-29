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


# Sleep Regularity Index (SRI) 
What is it? It's a measure of sleep regularity by Phillips et al. https://www.nature.com/articles/s41598-017-03171-4 It basically states how consistent your sleep is between two given days on a scale of 100. 100 means you are extremely regular, or in awake state and sleep state consistently at the same times between two days. 0 means you are extremely irregular and that you either were in awake state in day t and complete sleep state in day t+1 or vice versa.

# Modified Sleep Regularity Index (mSRI)
This is the average cumulative differences between SRI scores. It is to implicitly show if your regularity fluctuates a lot, or volatility of your regularity. 

# Sleep Regularity Index - Code Structure 
* All testcases are in `SleepRegularityIndexTest.java`. 
* For integration with the pre-existing app, the SRI is called under `getSleepIrregularity()` under the file `SocialJetlagStats.java`.
- `sriUtils.getSleepIrregularity(records)` calculates the average SRI scores 
- `sriUtils.getSleepIrregularityModified(records)` calculates the average MSRI scores 
* The `SleepRegularityIndexUtil.java` file includes the actual implementation of the Sleep Regularity Index. 
- It has a process in which it discretizes each 24 hour day to 1440 min representation BitSet 
- Let's say there exists BitSet b. If someone is awake at time 14:05, then it's at the 845th minute of the day (14*60 + 5). So b[i] is set to 1. Else, if the person is asleep then the value at b[i] would be 0.  

## SRI Total Calcuation 
1. We group all continuous dates and calculate SRI scores. Let's say there is 11-20, 11-21, 11-22, 11-25, 11-26. 
2. Then this is grouped into two. Group1: ( 11-20, 11-21, 11-22) and Group 2: ( 11-25, 11-26 ). 
3. Since we can only calculate SRI from pairs of days, Group1 will give us 2 SRI scores sri_1, sri_2. Group 2 will give us 1 SRI score sri_3. In total, there are 3 SRI scores. 
4. So, we calculate the average as the following: (sri_1 + sri_2 + sri_3)/3. 

## mSRI Total Calculation 
1. Let's say there is 11-20, 11-21, 11-22, 11-25, 11-26, 11-27, 11-29, 11-30. 
2. We first calculate SRI scores so we group the dates as follows: Group1: (11-20, 11-21, 11-22), Group 2: ( 11-25, 11-26, 11-27), Group 3: (11-29, 11-30). This results in the following SRI Scores: Group1 : (sri_1, sri_2), Group2:(sri_3, sri_4), Group3: (sri_5). 
3. The mSRI score requires 2 consecutive SRI scores. So the mSRI score for Group1 will result in one msri score msri_1 = sri_1 - sri_2 and for Group2 msri_2 = sri_3 - sri_4. 
4. Since there is only one SRI score, for GROUP 3, it will not be incorporated into the mSRI. Since there are only 2 mSRIs, the mSRI is then: (msri_1+msri_2)/2. 



