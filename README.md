# SafeHomePrototype

#Issues
## IDE related
###1. How to enable "assert" in intellij
[Stackoverflow Link.](https://stackoverflow.com/questions/18168257/where-to-add-compiler-options-like-ea-in-intellij-idea)
The **-ea** option to enable assertions is passed to the JVM (not to the compiler). Add it as a VM option 
for your runtime configuration. Specific location in the menu: *Run > Edit Configurations... > Configuration > VM options*

#Useful Links
- [Git readme Basic writing and formatting syntax](https://help.github.com/en/articles/basic-writing-and-formatting-syntax)
- [Threading issue](https://codereview.stackexchange.com/questions/56428/run-different-methods-in-background-threads-without-duplication)
- [JVM Shutdown Hook in Java](https://www.geeksforgeeks.org/jvm-shutdown-hook-java/)
-

#Adding devices
JSON FILE: /conf/deviceList.json

example:

    {"devName":"burner","devType":"DUMMY_DEVICE","ipAddr":"a.b.c.d","port":1234,"isPersistentTCP":true},
    {"devName":"smart_plug","devType":"TPLINK_HS110","ipAddr":"192.168.0.44","port":1234,"isPersistentTCP":true}

###Device Types
Currently There are two type of devices: 1) DUMMY_DEVICE and 2) TPLINK_HS110 (it should work for similar tp-link switches e.g. HS105)

The project "DeviceManager" maintains a Factory Design pattern, where based on the device type it will provide the corresponding driver.

###How to add new device support (e.g. WEMO):
    1. Add the corresponding device driver inside the "DeviceManager" project (e.g. driver for wemo)
    2. Register it into the FactoryClass
    3. Mention the device type into the JSON class
    Tips: Here is a (JAVA-driver) for WEMO, you might modify it and add it into the DeviceManager factory
    https://github.com/palominolabs/belkin-wemo-switch
    
###JSON Fields
    devName: unique name of the device
    devType: based on this type, the Factory class will provide the corresponding driver (currently supports DUMMY_DEVICE and TPLINK_HS1100)
    ipAddr: for DUMMY_DEVICE, fill it up with a dummy IP. For real device, fill it up with the real ip
    isPersistentTCP: if true, this software will hold the connection. Otherwise, it will create a new connection before each use.
    
#Adding Routines
JSON FILE: /conf/routineList.json
    
    Make sure to initialize/declare all devices in the deviceList.json file
    
    
#Safety Properties
JSON FILE: /conf/safetyList.json

    declare the safety rules
    
#Using real devices

    Communicating with a real smart-device requires a real IP address.
    
    How to know your device's IP address?
        * You can get it from your mobile app (in case of TP link, it is KASA)
        * You can get it from your router's setup page (typically the address is 191.168.0.1)
        
    You need to bind your device's IP address with the MAC address. Otherwise, the IP might get changed over time.
    
    How to bind an IP address:
        * Depends on your router. Each router has its own way. Here is a generic guideline for TP-Link routers
        https://www.youtube.com/watch?v=hC_o3kMjHa0
        
        
        
#How to run the program
    
    Initialize/declare your devices/routines (described above). Go to src/main/java/Main.java  and follow the sample codes.
    
    
    
#Code Flow

    Routines-> SafeHomeManager <=> RoutineManager <=> ConcurrencyManager <=> SafetyChecker <=> DeviceManager <=> Real/Dummy-Devices
    
    
# Quick Deployment Guide to run with TP-Link HS103
Setting up steps:
- For each TP-Link HS103, prepare them with their MAC address.
- Access your router, find each HS103 switch by its MAC address and fix
  their ip addresses.
- Setup all TP-Link HS103 switches with Kasa app (e.g. through phone). 
  The switches should be the same wifi network with your central device 
  (e.g. Raspberry Pi, laptop, etc). 
- Goes to `conf/deviceList.json` in this repo, for the device that you'd
  like your switch to react to, change the device type to `TPLINK_HS110`
  and change its corresponding ip address to what you've setup in the 
  router.
- Goes to `src/main/java/Main.java`, setup `parent_folder` (around line 151)
  to the absolute path that you'd like to save the experimental result.
- Starts run code of this repo.
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    