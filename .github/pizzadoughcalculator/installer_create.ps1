# PowerShell script to build Windows installer
param(
    [string]$JavaFXPath = "C:\javafx-sdk-24.0.1\lib"
)

Write-Host "Building Pizza Dough Calculator Windows Installer..." -ForegroundColor Green

# Clean and compile
Write-Host "Cleaning and compiling..." -ForegroundColor Yellow
mvn -e clean compile

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}

# Create runtime image
Write-Host "Creating runtime image..." -ForegroundColor Yellow
mvn javafx:jlink

if ($LASTEXITCODE -ne 0) {
    Write-Host "Runtime image creation failed!" -ForegroundColor Red
    exit 1
}

# Create installer
Write-Host "Creating Windows installer..." -ForegroundColor Yellow
jpackage `
    --type msi `
    --name "Pizza Dough Calculator" `
    --app-version "1.0.0" `
    --vendor "YourName" `
    --description "A JavaFX application to calculate pizza dough ingredients" `
    --module-path "$JavaFXPath" `
    --add-modules javafx.controls,javafx.fxml `
    --module C:\Users\Fuligin\IdeaProjects\pizzadoughcalculator `
    --dest target/installer `
    --win-dir-chooser `
    --win-menu `
    --win-shortcut `
    --icon src/main/resources/icon.ico

if ($LASTEXITCODE -eq 0) {
    Write-Host "Installer created successfully in target/installer/" -ForegroundColor Green
} else {
    Write-Host "Installer creation failed!" -ForegroundColor Red
}