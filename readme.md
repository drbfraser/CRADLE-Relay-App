

## Setup

### First-time setup

1. Set up and run the [Cradle Mobile](https://github.sfu.ca/bfraser/415-Cradle-Platform/blob/main/docs/development.md) project. The Docker image must be running to provide a back-end for this app.
1. Download, install, and open Android studio
1. Clone this repo to your computer and open it in Android Studio
1. Edit the gradle version in Android Studio
   - File -> Project Structure -> Project
      - Android Gradle Plugin Version: `8.1.4`
      - Gradle Version: `8.2`
1. Edit the JDK version in Android Studio
   - File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle
      - Gradle JDK: `corretto 17`
      - If it's not one of the options, click on "Download JDK" and select the appropriate version
1. Run the commands listed below to establish Git Pre-push hooks

### First-time running

1. Start two emulated phones that use `API 30` *See note, below
1. Check which phone has the phone number ending in `554` and which ends in `556` by checking LogCat
1. Run Cradle **SMS Relay** on the `554` phone
   - Phone numbers are currently hard-coded, and should be made dynamic
   - Connect the app to the (running) Cradle Platform from the login settings (top right):
      - Hostname: 10.0.2.2
      - Port: 5000
      - Use HTTPS: OFF
      > INFO:  
      > * IP 10.0.2.2 is the address that Android Studio forwards to the emulator in place of localhost.  
      > * Port 5000 is the port that the Flask container deploys to.
1. Run Cradle **Mobile** on the `556` phone
   - See the [Cradle Mobile](https://github.sfu.ca/bfraser/415-Cradle-Platform/blob/main/docs/development.md) docs
1. Re-name the emulated phones to reflect which has each respective app installed

### Every-time running

1. Start the emulated phone that runs **SMS Relay**
   - Check that it has a phone number ending in `554` by checking LogCat
1. Start the emulated phone that runs **Mobile**
   - Check that it has a phone number ending in `556` by checking LogCat
1. Start the **SMS Relay** and **Mobile** apps in their respective phones
   - If there is an issue with connection, check the settings above


## Pre-push hooks

Follow these steps before pushing commits.

### For Mac, run the following command  
  
     ln -s -f ../../hooks/pre-push.sh .git/hooks/pre-push  
  
### For Windows Terminal, run the following command as an Admin  

     mklink .git\hooks\pre-push ..\..\hooks\pre-push.sh  

### For Windows PowerShell, run the following command as an Admin  
     New-Item -ItemType SymbolicLink -Path .\.git\hooks -Name pre-push -Value .\hooks\pre-push.sh

## Notes 

`SMS Relay` works on APIs up to `API 30` when using the emulator. 
This is because `API 34` does not generate unique phone numbers for each emulated phone. 
`SMS Relay` has yet to be tested on real phones with API above 30.
