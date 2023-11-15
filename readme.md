# Satochip-DIY

A collection of tools, software and documentation to build and flash Satochip, Satodime and Seedkeeper Javacard based DIY Crypto hardware devices.

## Why Satochip?
Many DIY options for hardware wallets involve major trade-offs for usability (Some are stateless) and also physical security. Furthermore, most DIY options use general purpose micro-controllers, sometimes without any ability to secure the physical hardware. The challenge is that most hardware that is security hardened is only available for development under NDA, both limiting it's usabulity in DIY applications while also limiting vendors ability to disclose vulnerabilities they discover.

_Java Card is the leading open, interoperable platform for secure elements, enabling smart cards and other tamper-resistant chips to host multiple applications using Java technology. Java Card is an execution platform that can store and update multiple applications on a single resource-constrained device, while retaining the highest certification levels and compatibility with standards._
**_Oracle (Company that Defines the Javacard Specification)_**

Fortunatly, Java Card devices provide a platform that lends itself to Open Source (and therefore DIY) development, allowing for development of software that can run on hardened platform with a Secure Element, without needing an NDA, while still using commodity hardware that can be sourced from a number of different vendors in a range of form factors.

### Hardware Selection

#### Tested and Working
The following cards are readily available and tested and confirmed to work.

* NXP JCOP4 P71 Based Javacards
  * J3R110
  * J3R180 (Recommended Card) [Buy from Satochip](https://satochip.io/product/card-for-diy-project/)
* NXP JCOP3 P60 Based Javacards
  * J3H145
* THD-89 Based Javacards
  * CodeWav NFC Sticker Tag Micro Edition
  * THETAKey T101 
  * THETAKey T104 (CodeWav-2 NFC Card)

#### Javacard Features Required

The list of tested cards above isn't exhaustive and generally speaking a Javacard needs to support the following features:

* Javacard 3.0.4 (Or higher)
* Support the following Functions
  * javacard.security.KeyAgreement: ALG_EC_SVDP_DH_PLAIN, ALG_EC_SVDP_DH_PLAIN_XY
  * javacard.security.Signature: ALG_ECDSA_SHA_256
  * javacard.security.MessageDigest: ALG_SHA_256, ALG_SHA_512
  * javacard.security.RandomData: ALG_SECURE_RANDOM
  * javacardx.crypto.Cipher: ALG_AES_BLOCK_128_ECB, ALG_AES_BLOCK_128_CBC_NOPAD

You can find a database of various Javacards and their supported functions here: https://www.fi.muni.cz/~xsvenda/jcalgtest/table.html

**If you attempt to flash the applets to an unsupported card, you will likely get an error after the CAP file has been loaded.**

#### Where to Buy

You can source these Java Smartcards from a range of different websites depending on where in the world you live. Generally speaking, purchasing single cards is rarely cost-effective and you will often end up paying $20+ USD on shipping.

**If you want to be sure that the card will work and support development of this project, it is best to source the card through SatoChip... (Their pricing is quite competitive, especially for a single card)**

### Obtaining Applets to Flash to Javacards

#### Easiest Method - Downloading Official Releases
The fastest and simplest method is to simply flash CAP files from the releases.

[Official Satochip Releases](https://github.com/Toporin/SatochipApplet/releases)

[Official Seedkeeper Releases](https://github.com/Toporin/Seedkeeper-Applet/releases)

[Official Satodime Releases](https://github.com/Toporin/Satodime-Applet/releases)

_For unofficial builds (eg: THD-89 devices) you will need to either build the applets yourself or download them from Github Actions..._

#### Easy Method - Downloading from Github Actions
This repository automatically builds the official and THD-89 builds using Github Actions.

If you have a Github account and are logged in, you can view and download the build artifacts (Compiled applets, ready to flash) by clicking the button below, selecting the most recent run and scrolling down to the `Artifacts` and downloading a zip file contining all of the compiled applets.

[![Javacard Build](https://github.com/3rdIteration/Satochip-DIY/actions/workflows/ant.yml/badge.svg)](https://github.com/3rdIteration/Satochip-DIY/actions/workflows/ant.yml)

On this screen, you can also click on the `Build` job button to view a log of the build process and verify that the SHA256 sums of the applets match those that you downloaded.

#### Harder Method - Building on your own PC

##### Cloning this Github Repository
This repository includes resources from other Github repositories, so it is nessesary to use Git to download it recursively.

**Simply downloading the zip file directly from Github will not work...**

If your system has Github for Windows installed, you can simply click the `Code` button on this page and select `Open with GitHub Desktop`

If your system already has Git installed, you can open a terminal and type

    git clone --recursive https://github.com/3rdIteration/Satochip-DIY.git

##### Download and Unzip Apache Ant
If you are in Linux, you can normally install ant via apt (`sudo apt install ant`). 

If you are in Windows, you can download a binary distribution of Ant here: https://ant.apache.org/bindownload.cgi you can then unzip it and then run Ant via ant.bat that comes with it.

_The examples below assume that you have extracted Ant to your C: drive_

##### Download and Install Java SDK 8

While there are far newer versions of the Java SDK available, Java 8 supports the widest number of Javacard SDKs and has Long Term Support until 2030, so this is the best version to use when setting up a Javacard Build Environment...

You can find out more about supported Java versions here: https://github.com/martinpaljak/ant-javacard/wiki/JavaCard-SDK-and-JDK-version-compatibility

**On Windows**

You can download OpenJDK 8 here: https://adoptium.net/temurin/releases/?version=8

Once downloaded, you can install it with all the defaults.

**On Linux**
    
    sudo apt install openjdk-8

#### Building

Open a command prompt (or terminal) and navigate to the folder of this Github Repository.

Once there, you can simply run Ant.

On Windows (Assuming you unzipped the Ant Binary distribution onto C drive, in this example it is for version 1.9.16...)

    "C:\apache-ant-1.9.16\bin\ant.bat"

On Linux
    
    ant

Once complete, you will notice that there is a new folder that was created called `Build` which will contain all of the compiled applets.

### Flashing Applets to Javacards
This repository includes a release of [GlobalPlatformPro](https://github.com/martinpaljak/GlobalPlatformPro) which can be used to flash the applets. 

**On Windows**

    gp.exe --install FILENAME.cap

**On Other Operating Systems**

    java -jar gp.jar --install FILENAME.cap

### Locking Javacards (Optional)
While it is not possible download applets (And wallet data) from an unlocked card, leaving the card unlocked makes it very easy for someone to discover what applets are installed on the card, delete these applets and potentially install other applets of their own. (Including those which could exploit yet-to-be-discovered weaknesses in the Javacard OS or Applet segregation protections) Locking cards is easy and is a good idea...

To lock the cards, you simply run the `--lock` command with a 32 character hexidecimal key. 

**Be sure to notes this key down, as you will need it if you want to perform any install/uninstall/unlock operations on the card.**

For example, to lock the cards with the key `010B0371D78377B801F2D62AFC671D95`

**On Windows**

    gp --lock 010B0371D78377B801F2D62AFC671D95

**On Other Operating Systems**

    java --lock 010B0371D78377B801F2D62AFC671D95

After this command has been run, further operations will require that the key is specified with the `--key` argument

For example installing an applet on a card locked with the key `010B0371D78377B801F2D62AFC671D95` would be:

**On Windows**

    gp.exe --key 010B0371D78377B801F2D62AFC671D95 --install FILENAME.cap

**On Other Operating Systems**

    java -jar gp.jar --key 010B0371D78377B801F2D62AFC671D95 --install FILENAME.cap

### Simulator
