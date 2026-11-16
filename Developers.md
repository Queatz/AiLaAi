# Local development guide

To test the Android app against a local database, you need three pieces running together:

1. **ArangoDB** (the database)
2. **The Ktor backend** in `backend/`
3. **The Android app** in `android/` pointed at your local backend instead of production (`https://api.ailaai.app`)

---

## Prerequisites

Make sure you have **JDK 25**, `curl`, and Android Studio (with the Android SDK) installed.

### Ubuntu / Debian
```bash
sudo apt update
sudo apt install -y openjdk-25-jdk curl android-sdk-platform-tools
```

### RHEL / CentOS / Fedora
```bash
sudo dnf install -y java-25-openjdk-devel curl android-tools
```

*(Note: On RHEL/CentOS, enable EPEL first if `android-tools` is not in the default repositories, or use `adb` from `$HOME/Android/Sdk/platform-tools/adb` installed by Android Studio.)*

*(Optional — JetBrains / Android Studio JDK pinning: `android/gradle/gradle-daemon-jvm.properties` requires Java 25 from any vendor so your system JDK works out of the box. If you prefer Android Studio / JetBrains Runtime 25 specifically, run `cd android && ./gradlew updateDaemonJvm --jvm-version=25 --jvm-vendor=JETBRAINS` locally, or re-add `toolchainVendor=JETBRAINS` to `android/gradle/gradle-daemon-jvm.properties`.)*

---

## 1. Start ArangoDB locally

`backend/src/main/kotlin/com/queatz/db/Db.kt` connects to `127.0.0.1:8529` with database `ailaai`, user `ailaai`, and password `ailaai`, and automatically creates all collections and graphs on startup.

Choose either **Option A (Container: Podman / Docker)** or **Option B (Native package)**.

### Option A: Run ArangoDB in a container (recommended)

#### RHEL / CentOS / Fedora — Option 1: Podman (recommended)
```bash
sudo dnf install -y podman

podman run -d --name ailaai-arango \
  -p 8529:8529 \
  -e ARANGO_ROOT_PASSWORD=ailaai \
  docker.io/library/arangodb:3.12
```

Wait a few seconds for ArangoDB to start, then create the `ailaai` database and user:
```bash
podman exec -i ailaai-arango arangosh \
  --server.endpoint tcp://127.0.0.1:8529 \
  --server.username root \
  --server.password ailaai <<'EOF'
const users = require("@arangodb/users");
db._createDatabase("ailaai");
users.save("ailaai", "ailaai");
users.grantDatabase("ailaai", "ailaai", "rw");
EOF
```

#### RHEL / CentOS / Fedora — Option 2: Docker
```bash
# Fedora:
sudo dnf install -y moby-engine
# Or RHEL / CentOS (using Docker CE repo) / Fedora:
# sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
# sudo dnf install -y docker-ce docker-ce-cli containerd.io
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
# Log out and back in (or run `newgrp docker`) for group changes to apply

docker run -d --name ailaai-arango \
  -p 8529:8529 \
  -e ARANGO_ROOT_PASSWORD=ailaai \
  arangodb:3.12
```

Wait a few seconds for ArangoDB to start, then create the `ailaai` database and user:
```bash
docker exec -i ailaai-arango arangosh \
  --server.endpoint tcp://127.0.0.1:8529 \
  --server.username root \
  --server.password ailaai <<'EOF'
const users = require("@arangodb/users");
db._createDatabase("ailaai");
users.save("ailaai", "ailaai");
users.grantDatabase("ailaai", "ailaai", "rw");
EOF
```

#### Ubuntu / Debian (Docker)
```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
# Log out and back in (or run `newgrp docker`) for group changes to apply

docker run -d --name ailaai-arango \
  -p 8529:8529 \
  -e ARANGO_ROOT_PASSWORD=ailaai \
  arangodb:3.12
```

Wait a few seconds for ArangoDB to start, then create the `ailaai` database and user:
```bash
docker exec -i ailaai-arango arangosh \
  --server.endpoint tcp://127.0.0.1:8529 \
  --server.username root \
  --server.password ailaai <<'EOF'
const users = require("@arangodb/users");
db._createDatabase("ailaai");
users.save("ailaai", "ailaai");
users.grantDatabase("ailaai", "ailaai", "rw");
EOF
```

### Option B: Install ArangoDB natively

#### Ubuntu / Debian
```bash
curl -OL https://download.arangodb.com/arangodb312/DEBIAN/Release.key
sudo apt-key add - < Release.key
echo 'deb https://download.arangodb.com/arangodb312/DEBIAN/ /' | sudo tee /etc/apt/sources.list.d/arangodb.list
sudo apt update
sudo apt install -y apt-transport-https arangodb3
sudo systemctl enable --now arangodb3
```

#### RHEL / CentOS / Fedora
```bash
sudo tee /etc/yum.repos.d/arangodb.repo <<'EOF'
[arangodb]
name=ArangoDB
baseurl=https://download.arangodb.com/arangodb312/RPM/
enabled=1
gpgcheck=1
gpgkey=https://download.arangodb.com/arangodb312/RPM/repodata/repomd.xml.key
EOF

sudo dnf install -y arangodb3
sudo systemctl enable --now arangodb3
```

*(If the `arangodb3` RPM package prompts for a root password or defaults to blank, set/use your root password when running `arangosh` below.)*

#### Create the `ailaai` database and user
You can either open `http://127.0.0.1:8529` in your browser and create the `ailaai` database and `ailaai` user (password `ailaai`, read/write on `ailaai`), or run via `arangosh`:

```bash
arangosh --server.endpoint tcp://127.0.0.1:8529 --server.username root <<'EOF'
const users = require("@arangodb/users");
db._createDatabase("ailaai");
users.save("ailaai", "ailaai");
users.grantDatabase("ailaai", "ailaai", "rw");
EOF
```

---

## 2. Start the local backend

1. Create `backend/secrets.json` matching the `Secrets` schema in `backend/src/main/kotlin/com/queatz/Secrets.kt` (loaded by `backend/src/main/kotlin/com/queatz/plugins/Vars.kt`). For local testing without push notifications, AI providers, or VideoSDK, placeholder strings work for most fields, with a non-empty `jwt.secret`:

   ```bash
   cat <<'EOF' > backend/secrets.json
   {
     "config": { "domain": "http://localhost:8080" },
     "jwt": { "secret": "local-dev-secret" },
     "hms": { "appId": "", "clientId": "", "clientSecret": "" },
     "gms": { "appId": "", "clientId": "", "clientEmail": "", "privateKeyId": "", "privateKey": "" },
     "apns": { "privateKey": "", "privateKeyId": "", "teamId": "", "topic": "" },
     "dezgo": { "key": "" },
     "openAi": { "key": "" },
     "videoSdk": { "apiKey": "", "secret": "" },
     "google": { "apiKey": "" }
   }
   EOF
   ```

2. Start the server from `backend/` (or run `./r`):

   ```bash
   cd backend
   ./gradlew :run
   ```

3. The server binds to `0.0.0.0:8080` (`backend/src/main/kotlin/com/queatz/Application.kt`). In another terminal, verify it is running:

   ```bash
   curl -s http://127.0.0.1:8080/hi
   # Expected output: { "hi": true }
   ```

   *(Note on logging: Netty logging is set to `INFO` in `backend/src/main/resources/logback.xml` to avoid noisy HTTP/2 transport frames and internal JDK `sun.misc.Unsafe` probe warnings on Java 9+. If you ever need deep Netty wire or buffer diagnostics, switch `<logger name="io.netty" level="INFO"/>` back to `DEBUG`.)*

---

## 3. Configure the Android app in Android Studio

1. **Open the `android/` directory** as the project root in Android Studio (it is a standalone Gradle build that includes `../shared`).
2. **Create `android/local.properties`** from `android/local.properties.example` and set `sdk.dir` to your Android SDK path:

   ```bash
   # From the android/ directory:
   cp local.properties.example local.properties
   ```

   Edit `android/local.properties` (or let Android Studio populate `sdk.dir`), ensuring `sdk.dir` is set (for example, `sdk.dir=/home/<user>/Android/Sdk`) and `GOOGLE_MAPS_API_KEY=` / `HMS_APP_ID=` are present (they can remain empty if you aren't testing maps or Huawei push).

3. **Set up local HTTPS (`mkcert` + `caddy`):**
   `android/app/src/main/AndroidManifest.xml` keeps cleartext `http://` disabled (`targetSdk = 36`). Instead of enabling cleartext HTTP, the dedicated `local` build type (`isDebuggable = true`) includes `android/app/src/local/AndroidManifest.xml` and `android/app/src/local/res/xml/network_security_config.xml`, which uses `<debug-overrides>` to trust user-installed CA certificates (never included in `debug` or `release` builds).

   1. Install `mkcert` and `caddy`, then create a local CA and certificate for your local endpoints (add your `<YOUR_LAN_IP>` if testing over Wi-Fi):

      ```bash
      mkcert -install
      mkdir -p ~/.local/share/ailaai-tls
      cd ~/.local/share/ailaai-tls
      mkcert localhost 127.0.0.1 10.0.2.2
      ```

   2. Start Caddy in front of the local backend on port `8443` (Caddy terminates TLS and proxies both HTTPS and `wss://` WebSockets to `127.0.0.1:8080`):

      ```bash
      caddy start --adapter caddyfile --config - <<'EOF'
      {
          auto_https disable_redirects
      }

      localhost:8443, 127.0.0.1:8443, 10.0.2.2:8443 {
          tls ~/.local/share/ailaai-tls/localhost+2.pem ~/.local/share/ailaai-tls/localhost+2-key.pem
          reverse_proxy 127.0.0.1:8080
      }
      EOF
      ```

   3. Install your `mkcert` root CA (`"$(mkcert -CAROOT)/rootCA.pem"`) once on your emulator or test device:
      - Copy `rootCA.pem` to the device:
        ```bash
        adb push "$(mkcert -CAROOT)/rootCA.pem" /sdcard/Download/mkcert-rootCA.crt
        ```
      - On the emulator/phone, open **Settings → Security & privacy → More security & privacy → Encryption & credentials → Install a certificate → CA certificate**, choose **Install anyway**, and select `Download/mkcert-rootCA.crt`.

4. **Point `Api` to your local HTTPS backend:**  
   In `android/app/src/main/java/com/queatz/ailaai/data/Api.kt`, switch `baseUrl` from production to your local HTTPS proxy:

   ```kotlin
   // override val baseUrl = "https://api.ailaai.app"
   override val baseUrl = "https://10.0.2.2:8443"
   ```

   (`android/app/src/main/java/com/queatz/ailaai/services/Assistant.kt` also derives its `wss://` WebSocket URL from `api.baseUrl`, so WebSockets switch automatically.)

---

## 4. Run on the Emulator first

1. Keep `override val baseUrl = "https://10.0.2.2:8443"` in `android/app/src/main/java/com/queatz/ailaai/data/Api.kt` (`10.0.2.2` maps to your host machine's `127.0.0.1` inside the Android Emulator).
2. In Android Studio's **Build Variants** tool window, select `googleLocal` (recommended for Google Play emulators) or `defaultLocal`.
3. Select your emulator in the device dropdown and click **Run** (`Shift+F10`), or install from the terminal:

   ```bash
   cd android
   ./gradlew installGoogleLocal
   ```

> **Note:** `android/app/build.gradle.kts` sets `isDebuggable = false` on `debug` builds for Compose performance. The `local` build type sets `isDebuggable = true` so both `<debug-overrides>` (user CA trust) and attaching breakpoints with **Debug** work out of the box.

---

## 5. Run on a Real Device next

Enable **Developer Options** and **USB Debugging** on your phone, connect it to your computer via USB, and verify `adb` sees it:

```bash
adb devices
```

*(If `adb` is not in your `PATH`, use `$HOME/Android/Sdk/platform-tools/adb`.)*

Make sure `mkcert`'s `rootCA.pem` is installed as a CA certificate on the phone (see step 3.3 above). Then choose one of two options:

### Option A: Over USB with `adb reverse` (easiest, works through firewalls)

1. Forward port `8443` from the phone to your computer:

   ```bash
   adb reverse tcp:8443 tcp:8443
   ```

2. Set `baseUrl` in `android/app/src/main/java/com/queatz/ailaai/data/Api.kt` to:

   ```kotlin
   override val baseUrl = "https://127.0.0.1:8443"
   ```

   *(Tip: `adb reverse tcp:8443 tcp:8443` + `https://127.0.0.1:8443` works for **both** the emulator and a USB-connected phone without editing `Api.kt` between runs.)*

3. Select your physical device in Android Studio and click **Run** (or run `cd android && ./gradlew installGoogleLocal`).

### Option B: Over Wi-Fi (LAN IP)

1. Connect your computer and phone to the same Wi-Fi network.
2. Find your computer's local IP address:

   ```bash
   hostname -I | awk '{print $1}'
   ```

3. Include `<YOUR_LAN_IP>` in your `mkcert` certificate and Caddy site list (`<YOUR_LAN_IP>:8443`), and open port `8443` in your firewall if one is active:

   **Ubuntu / Debian (`ufw`):**
   ```bash
   sudo ufw allow 8443/tcp
   ```

   **RHEL / CentOS / Fedora (`firewalld`):**
   ```bash
   sudo firewall-cmd --add-port=8443/tcp --permanent
   sudo firewall-cmd --reload
   ```

4. Set `baseUrl` in `android/app/src/main/java/com/queatz/ailaai/data/Api.kt` to:

   ```kotlin
   override val baseUrl = "https://<YOUR_LAN_IP>:8443"
   ```

5. Select your physical device in Android Studio and click **Run**.
