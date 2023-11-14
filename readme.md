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

* NXP JCOP4 P71
  * J3R180 (Recommended Card) [Buy from Satochip](https://satochip.io/product/card-for-diy-project/)
* NXP JCOP3 P60
  * J3H145
* THD-89 Based Java Smartcards
  * CodeWav NFC Sticker Tag Micro Edition
  * THETAKey T101 
  * THETAKey T104 (CodeWav-2 NFC Card)

#### Javacard Features Required

The list of tested cards above isn't exhaustive and generally speaking a Javacard needs to support the following features:

* Javacard 3.0.4 (Or higher)
* javacard.security.KeyAgreement: ALG_EC_SVDP_DH_PLAIN, ALG_EC_SVDP_DH_PLAIN_XY
* javacard.security.Signature: ALG_ECDSA_SHA_256
* javacard.security.MessageDigest: ALG_SHA_256, ALG_SHA_512
* javacard.security.RandomData: ALG_SECURE_RANDOM
* javacardx.crypto.Cipher: ALG_AES_BLOCK_128_ECB, ALG_AES_BLOCK_128_CBC_NOPAD

You can find a database of various Javacards and their supported functions here: https://www.fi.muni.cz/~xsvenda/jcalgtest/table.html

**_If you attempt to flash the applets to an unsupported card, you will likely get an error after the CAP file has been loaded.**_ 

#### Where to Buy

You can source these Java Smartcards from a range of different websites depending on where in the world you live. Generally speaking, purchasing single cards is rarely cost-effective and you will often end up paying $20+ USD on shipping.

**If you want to be sure that the card will work and support development of this project, it is best to source the card through SatoChip... (Their pricing is quite competitive, especially for a single card)**

### Obtaining Applets to Flash to Javacards

#### Easiest Method - Downloading Official Releases
The fastest and simplest method is to simply flash CAP files from the releases.

[Official Satochip Releases](https://github.com/Toporin/SatochipApplet/releases)
[Official Seedkeeper Releases](https://github.com/Toporin/Seedkeeper-Applet/releases)
[Official Satodime Releases](https://github.com/Toporin/Satodime-Applet/releases)

#### Easy Method - Downloading from Github Actions
This repository automatically builds all of the official and THD-89 builds using Github Actions.

If you have a Github account and are logged in, you can view and download the build artifacts (Compiled applets, ready to flash) here.

#### Harder Method - Building on your own PC

If you want to build the CAP files on your own system, keep on reading...

### Flashing Applets to Javacards

gp --install FILENAME.cap

### Locking Javacards (Optional)

While it is not possible download applets (And wallet data) from an unlocked card, leaving the card unlocked makes it very easy for someone to discover what applets are installed on the card, delete these applets and potentially install other applets of their own. (Including those which could exploit yet-to-be-discovered weaknesses in the Javacard OS or Applet segregation protections) Locking cards is easy and is a good idea...

### Build Environment

### Simulator