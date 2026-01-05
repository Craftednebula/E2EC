# -------------------------------
# runAll.ps1
# -------------------------------

# Navigate to project root
cd "C:\Users\darwi\Desktop\E2EC"

# Number of clients to launch
$clientCount = 2  # Change this to launch more clients

# 1️⃣ Compile project
Write-Host "Compiling project..."
mvn clean compile

# 2️⃣ Start LookupServer
Write-Host "Starting LookupServer..."
Start-Process powershell -ArgumentList '-NoExit', '-Command', 'mvn exec:java "-Dexec.mainClass=org.crafted.e2ec.E2lookup.LookupServer"'

# Wait a few seconds for LookupServer to initialize
Start-Sleep -Seconds 3

# 3️⃣ Start DedicatedServer
Write-Host "Starting DedicatedServer..."
Start-Process powershell -ArgumentList '-NoExit', '-Command', 'mvn exec:java "-Dexec.mainClass=org.crafted.e2ec.DedicatedServer.Server"'

# Wait a few seconds for DedicatedServer to initialize
Start-Sleep -Seconds 3

# 4️⃣ Start clients
for ($i = 1; $i -le $clientCount; $i++) {
    Write-Host "Starting Client $i..."
    Start-Process powershell -ArgumentList '-NoExit', '-Command', "mvn exec:java `"-Dexec.mainClass=org.crafted.e2ec.E2client.Client`""
}

Write-Host "All processes started."
