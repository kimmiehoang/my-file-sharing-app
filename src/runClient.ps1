# Đường dẫn tới Java
$JavaPath = "C:\Program Files\Java\jdk-21\bin\java.exe"  # Đảm bảo là đường dẫn đúng

# Tên lớp Java
$JavaClass = "Client"
$tempFilePath = "E:\my-file-sharing-app\repo\testshell.txt"
$tempFile = $tempFilePath
# Tạo tệp tạm để chứa lệnh và kết quả
#$tempFile = [System.IO.Path]::GetTempFileName()
Write-Host "Temp file path: $tempFile"
$inputValue = "";
$inputValue | Out-File -FilePath $tempFile -Encoding ASCII
# Khởi chạy tiến trình Java và tạo process object để theo dõi
$JavaProcess = Start-Process -FilePath $JavaPath -ArgumentList "$JavaClass $tempFile" -PassThru -WindowStyle Hidden -ErrorAction Stop


# Chờ JavaProcess khởi động
Start-Sleep -Seconds 2

if ($null -eq $JavaProcess) {
    Write-Host "Error"
    exit
}

while ($true) {
    $output = Get-Content -Path $tempFile -Raw
    # Write-Host "Result: $output"
    if ($output -match "No Server found" -or $output -match "UnknownHost") {
        Write-Host "Error: $output"
        #$JavaProcess.WaitForExit()
        #Remove-Item -Path $tempFile -Force
        exit
    }
    if ($output -match "Start successfully") {
        Write-Host "Register your hostname first. Follow the command HOSTNAME name"
        break
    }
}

# Nhập các lệnh từ bàn phím và ghi vào tệp tạm
while ($true) {
    $command = Read-Host "Enter command (or type 'exit' to end program):"
    
    # Kiểm tra nếu người dùng muốn kết thúc
    if ($command -eq 'QUIT') {
        #Get-Process | Where-Object { $_.ProcessName -eq $JavaProcessName } | ForEach-Object { Stop-Process -Id $_.Id }
        $JavaProcess.WaitForExit()
        #Remove-Item -Path $tempFile -Force
        Get-Process | Where-Object { $_.MainWindowTitle -eq $JavaClass } | ForEach-Object { Stop-Process -Id $_.Id }
        exit
    }

    Write-Host "Sending command: $command"

    try {
        # Ghi lệnh vào tệp tạm
        $command | Out-File -FilePath $tempFile -Encoding ASCII

        # Chờ JavaProcess đọc và xử lý lệnh
        Start-Sleep -Seconds 3

        # Đọc kết quả từ tệp tạm
        $output = Get-Content -Path $tempFile -Raw

        # In kết quả
        Write-Host "Result: $output"

    }
    catch {
        Write-Host "Failed to start Java process. Error: $_"
        break
    }
}
