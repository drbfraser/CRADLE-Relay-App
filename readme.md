
## Pre-push hooks
#### For Mac, run the following command  
  
     ln -s -f ../../hooks/pre-push.sh .git/hooks/pre-push  
  
#### For Windows, run the following command as an Admin  

     mklink .git\hooks\pre-push ..\..\hooks\pre-push.sh  

#### For PowerShel, run the following command as an Admin  
     New-Item -ItemType SymbolicLink -Path .\.git\hooks -Name pre-push -Value .\hooks\pre-push.sh

# Setup

Spring 2024 Set Up:
   - File -> Project Structure -> Project
      - Android Gradle Plugin Version: 8.1.4
      - Gradle Version: 8.2
   - File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle
      - Gradle JDK: coretto 17
      - If it's not one of the options, click on "Download JDK" and select the version mentioned above

We were only able to get SMS Relay functionality to work properly on the emulators using API 30. 
Testing with older versions seemed to work fine too, but API 34 does not work because the emulator on API 34 does not generate unique phone numbers for each instance of an API 34 emulator. Where as on API 30, each instance of the emulator gets a unique phone number.

Whenever working with both apps, always make sure to start the emulator with the Cradle Mobile app first, then the emulator with the SMS Relay app. This is to ensure that the Cradle Mobile emulator always gets the phone number ending with 554 and the SMS Relay always gets the emulator with the phone number ending in 556.
