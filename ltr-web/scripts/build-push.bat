@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM Build and Push Docker Image Script for Land Title Registry Web Application
REM ============================================================================
REM This script builds the Docker image and pushes it to a container registry
REM Supports: AWS ECR and Docker Hub
REM ============================================================================

echo ============================================================================
echo   Land Title Registry - Docker Build and Push Script
echo ============================================================================
echo.

REM Project configuration
set PROJECT_NAME=ltr-web
set BUILD_CONTEXT=..
set DOCKERFILE_PATH=ltr-web\Dockerfile

REM Sanitize project name for Docker tag (lowercase, hyphenate)
set IMAGE_NAME=ltr-web

echo Project: %PROJECT_NAME%
echo Sanitized Image Name: %IMAGE_NAME%
echo.

REM Prompt for registry type
echo Select Container Registry:
echo   1. AWS ECR (Elastic Container Registry)
echo   2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "%REGISTRY_CHOICE%"=="1" (
    echo.
    echo --- AWS ECR Configuration ---
    set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
    set /p AWS_ACCOUNT_ID="Enter AWS Account ID: "
    set /p ECR_REPO="Enter ECR Repository Name (default: %IMAGE_NAME%): "
    if "!ECR_REPO!"=="" set ECR_REPO=%IMAGE_NAME%
    
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    
    echo.
    echo Authenticating with AWS ECR...
    for /f "delims=" %%i in ('aws ecr get-login-password --region !AWS_REGION!') do set ECR_PASSWORD=%%i
    echo !ECR_PASSWORD! | docker login --username AWS --password-stdin !REGISTRY_URL!
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: ECR authentication failed
        exit /b 1
    )
    
    echo Checking if ECR repository exists...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository: !ECR_REPO!
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    )
    
    set /p IMAGE_TAG="Enter image tag (default: latest): "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!
    
) else if "%REGISTRY_CHOICE%"=="2" (
    echo.
    echo --- Docker Hub Configuration ---
    set /p DOCKER_USERNAME="Enter Docker Hub Username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub Password/Token: "
    
    echo.
    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub authentication failed
        exit /b 1
    )
    
    set /p IMAGE_TAG="Enter image tag (default: latest): "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/%IMAGE_NAME%:!IMAGE_TAG!
    
) else (
    echo ERROR: Invalid choice. Please select 1 or 2.
    exit /b 1
)

echo.
echo ============================================================================
echo Building Docker Image
echo ============================================================================
echo Image: !FULL_IMAGE_NAME!
echo Build Context: %BUILD_CONTEXT%
echo Dockerfile: %DOCKERFILE_PATH%
echo.

docker build -f %DOCKERFILE_PATH% -t !FULL_IMAGE_NAME! %BUILD_CONTEXT%

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed
    exit /b 1
)

echo.
echo ============================================================================
echo Pushing Docker Image
echo ============================================================================
echo Pushing: !FULL_IMAGE_NAME!
echo.

docker push !FULL_IMAGE_NAME!

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed
    exit /b 1
)

echo.
echo ============================================================================
echo Build and Push Completed Successfully
echo ============================================================================
echo Image: !FULL_IMAGE_NAME!
echo.
echo Next Steps:
echo   1. Update kubernetes/deployment.yaml with image URI
echo   2. Run deploy-image.bat to deploy to AWS EKS
echo ============================================================================

endlocal
