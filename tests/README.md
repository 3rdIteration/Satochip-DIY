# jCardsim test suite

Automated, hardware-free tests for the six applet builds in this repository
(Satochip, Satodime and Seedkeeper, each in an *official* and a *THD-89* variant).

The applets are executed inside [jCardsim](https://github.com/ph4r05/jcardsim)
(`com.klinec:jcardsim:3.0.6.0`), a Java Card 3.0.5 simulator, and driven purely over APDUs
including the full Satochip/Satodime/Seedkeeper secure channel.

## Running

```bash
cd tests
./gradlew test          # all six variants + the cross-applet pairing tests
./gradlew :applet-satochip:test
./gradlew :applet-satochip-thd89:test
```

Requirements: JDK 8 (the build and CI use Temurin 8) and a recursive clone
(the JavaCard SDK lives in the `sdks` submodule).

## Layout

```
tests/
  testkit/
    common/           shared harness: Card, SecureChannel client, crypto/EC helpers, KATs
    tests-core/       transport tests that run for every applet
    tests-satochip/   Satochip test base + suites (shared by official and THD-89)
    tests-satodime/   Satodime test base + suites
    tests-seedkeeper/ Seedkeeper test base + suites + storage unit tests
    tests-integration/ Seedkeeper -> Satochip secure-import pairing test
  applet-satochip/        } one Gradle module per variant; each compiles the real
  applet-satochip-thd89/  } applet sources and runs the matching shared suites
  applet-satodime/        }
  applet-satodime-thd89/  }
  applet-seedkeeper/      }
  applet-seedkeeper-thd89/}
  integration-pairing/        official Satochip + Seedkeeper on one simulated card
  integration-pairing-thd89/  THD-89 Satochip + Seedkeeper
```

Each applet variant lives in its own module because the forks share class names
(`org.satochip.applet.CardEdge`, ...). Tests are written against the APDU interface
(plus a few white-box storage tests) so the same source runs for both forks.

## Coverage

* **Secure channel** — handshake (ECDH + ephemeral key recovery from the self-signature),
  wrapped command round-trips, wrong MAC / IV / length, uninitialized channel.
* **Satochip** — status, setup, PIN verify/change/unblock/lock-out, card label, BIP32 seed
  import/reset, authentikey, extended key derivation, message signing, 2FA
  set/reset/crypt, Seedkeeper trusted-pubkey + encrypted-secret import, PKI, challenge-response.
* **Satodime** — status, setup, keyslot seal/unseal/private-key/reset/transfer lifecycle,
  keyslot metadata, contactless unlock counter + HMAC unlock code, PKI, challenge-response.
* **Seedkeeper** — status, setup, PIN, labels, NFC policy, masterseed generation, plaintext
  import/export, header listing, reset, object/memory/log storage layer, authentikey, PKI.
* **Cross-applet** — Seedkeeper generates a seed, registers the Satochip authentikey,
  performs a secure export (`0xA8`), and the Satochip imports it (`0xAA` + `0xAC`).

## Regression tests for reported issues

* **Issue #3** (THD-89 `trusted_pubkey` never allocated): `SatochipSeedkeeperImportTest`
  and the pairing integration tests fail with `0x6F00` on the THD-89 build if the
  allocation in `complete_install()` is removed. The fix is applied in
  `applets/satochip-thd89/src/org/satochip/applet/CardEdge.java`.
* **Issue #2** (`sha256sums.txt` never verifies): fixed in `.github/workflows/ant.yml`;
  the manifest is generated from inside `build/` without listing itself, and the workflow
  now runs `sha256sum -c` on it.

## Notes and limitations

* jCardsim implements every cryptographic primitive the applets require, so it cannot
  reproduce real-hardware failures such as a card lacking `ALG_EC_SVDP_DH_PLAIN_XY`, CAP
  load/memory-fit errors, or side-channel/timing behaviour.
* Tests install applet classes directly (jCardsim does not consume `.cap` files); the CAP
  files themselves are still built and checksum-verified by the Ant CI job.
* THD-89 builds defer their heavy initialisation to `select()` (`complete_install()`); the
  fixtures always select the applet before talking to it, matching on-card behaviour.
