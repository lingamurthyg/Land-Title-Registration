@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM Build and Push Docker Image Script
REM Land Title Registry System
REM Platform: Windows
REM ============================================================================

echo ============================================================================
echo   Land Title Registry - Docker Build and Push Script
echo ============================================================================
echo.

REM Project configuration
set PROJECT_NAME=land-title-registry
set IMAGE_NAME=land-title-registry

echo Project: %PROJECT_NAME%
echo Sanitized Image Name: %IMAGE_NAME%
echo.

REM Prompt for registry type
echo Select Docker Registry:
echo   1. AWS ECR (Elastic Container Registry)
echo   2. Docker Hub
echo.
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    REM AWS ECR Configuration
    echo.
    echo AWS ECR Configuration
    echo ----------------------------------------
    
    set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
    set /p AWS_ACCOUNT_ID="Enter AWS Account ID: "
    set /p ECR_REPO="Enter ECR Repository Name [%IMAGE_NAME%]: "
    if "!ECR_REPO!"=="" set ECR_REPO=%IMAGE_NAME%
    
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    
    echo.
    echo Registry URL: !REGISTRY_URL!
    echo Repository: !ECR_REPO!
    
    REM Prompt for image tag
    set /p IMAGE_TAG="Enter image tag [latest]: "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!
    
    echo.
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Authenticate with AWS ECR
    echo Authenticating with AWS ECR...
    for /f "delims=" %%i in ('aws ecr get-login-password --region !AWS_REGION!') do set ECR_PASSWORD=%%i
    echo !ECR_PASSWORD! | docker login --username AWS --password-stdin !REGISTRY_URL!
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: ECR authentication failed
        exit /b 1
    )
    
    echo [SUCCESS] ECR authentication successful
    
    REM Check if ECR repository exists, create if not
    echo.
    echo Checking ECR repository...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Repository does not exist. Creating...
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        if !ERRORLEVEL! neq 0 (
            echo ERROR: Failed to create ECR repository
            exit /b 1
        )
        echo [SUCCESS] ECR repository created
    )
    
) else if "!REGISTRY_CHOICE!"=="2" (
    REM Docker Hub Configuration
    echo.
    echo Docker Hub Configuration
    echo ----------------------------------------
    
    set /p DOCKER_USERNAME="Enter Docker Hub Username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub Password/Token: "
    set /p DOCKER_REPO="Enter Docker Hub Repository [%IMAGE_NAME%]: "
    if "!DOCKER_REPO!"=="" set DOCKER_REPO=%IMAGE_NAME%
    
    REM Prompt for image tag
    set /p IMAGE_TAG="Enter image tag [latest]: "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!DOCKER_REPO!:!IMAGE_TAG!
    
    echo.
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Authenticate with Docker Hub
    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub authentication failed
        exit /b 1
    )
    
    echo [SUCCESS] Docker Hub authentication successful
    
) else (
    echo ERROR: Invalid choice. Please select 1 or 2.
    exit /b 1
)

REM Build Docker image
echo.
echo ============================================================================
echo Building Docker Image...
echo ============================================================================
echo.

docker build -t "!FULL_IMAGE_NAME!" .

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed
    exit /b 1
)

echo.
echo [SUCCESS] Docker build successful

REM Push Docker image
echo.
echo ============================================================================
echo Pushing Docker Image...
echo ============================================================================
echo.

docker push "!FULL_IMAGE_NAME!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed
    exit /b 1
)

echo.
echo [SUCCESS] Docker push successful

REM Summary
echo.
echo ============================================================================
echo Build and Push Complete!
echo ============================================================================
echo.
echo Image: !FULL_IMAGE_NAME!
echo.
echo Next steps:
echo   1. Update kubernetes/deployment.yaml with the image URI
echo   2. Run deploy-image.bat to deploy to AWS EKS
echo.

endlocal
