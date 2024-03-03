
## Pre-push hooks
#### For Mac, run the following command  
  
     ln -s -f ../../hooks/pre-push.sh .git/hooks/pre-push  
  
#### For Windows, run the following command as an Admin  

     mklink .git\hooks\pre-push ..\..\hooks\pre-push.sh  

#### For PowerShel, run the following command as an Admin  
     New-Item -ItemType SymbolicLink -Path .\.git\hooks -Name pre-push -Value .\hooks\pre-push.sh

# Setup

Spring 2024 Set Up:
   - Android Gradle Plugin Version: 3.6.3
   - Gradle Version: 6.3
   - Java Version: coretto 11
   
