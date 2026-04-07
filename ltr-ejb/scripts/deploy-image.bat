@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM AWS EKS Deployment Script for ltr-ejb (Windows)
REM Deploys containerized application to Amazon Elastic Kubernetes Service
REM ============================================================================

echo ==========================================
echo AWS EKS Deployment Script
echo Project: ltr-ejb
echo ==========================================
echo.

REM Prompt for AWS configuration
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS Region is required
    exit /b 1
)

set /p CLUSTER_NAME="Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS Cluster Name is required
    exit /b 1
)

REM Prompt for Docker image URI
echo.
echo Enter the full Docker image URI (including registry, repository, and tag)
echo Example: 123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-ejb:latest
set /p IMAGE_URI="Docker Image URI: "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker Image URI is required
    exit /b 1
)

REM Prompt for application-specific environment variables
echo.
echo === Application Configuration ===
echo Enter values for environment variables (press Enter to use defaults)
echo.

set /p DB_HOST="Database Host (default: database-host): "
if "!DB_HOST!"=="" set DB_HOST=database-host

set /p DB_PORT="Database Port (default: 5432): "
if "!DB_PORT!"=="" set DB_PORT=5432

set /p DB_NAME="Database Name (default: ltr_db): "
if "!DB_NAME!"=="" set DB_NAME=ltr_db

set /p DB_USER="Database User (default: ltr_user): "
if "!DB_USER!"=="" set DB_USER=ltr_user

set /p JNDI_DATASOURCE="JNDI DataSource (default: jdbc/LandTitleDB): "
if "!JNDI_DATASOURCE!"=="" set JNDI_DATASOURCE=jdbc/LandTitleDB

echo.
echo ==========================================
echo Configuring kubectl for EKS
echo ==========================================
echo Region: !AWS_REGION!
echo Cluster: !CLUSTER_NAME!
echo.

REM Configure kubectl to use EKS cluster
aws eks update-kubeconfig --region "!AWS_REGION!" --name "!CLUSTER_NAME!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for EKS cluster
    exit /b 1
)

REM Verify cluster connectivity
echo.
echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to Kubernetes cluster
    exit /b 1
)

echo.
echo ==========================================
echo Updating Kubernetes Manifests
echo ==========================================
echo.

REM Create temporary directory for processed manifests
set TEMP_DIR=%TEMP%\ltr-ejb-deploy-%RANDOM%
mkdir "!TEMP_DIR!"

REM Copy manifests to temp directory
xcopy /E /I /Y kubernetes "!TEMP_DIR!" >nul

REM Replace placeholders in deployment.yaml using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_USER}}', '!DB_USER!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{JNDI_DATASOURCE}}', '!JNDI_DATASOURCE!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo Manifests updated successfully
echo.

echo ==========================================
echo Deploying to AWS EKS
echo ==========================================
echo.

REM Apply Kubernetes manifests in order
echo Creating namespace...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"

echo.
echo Deploying application...
kubectl apply -f "!TEMP_DIR!\deployment.yaml"

echo.
echo Creating service...
kubectl apply -f "!TEMP_DIR!\service.yaml"

echo.
echo Creating ingress...
kubectl apply -f "!TEMP_DIR!\ingress.yaml"

echo.
echo ==========================================
echo Waiting for Deployment Rollout
echo ==========================================
echo.

REM Wait for deployment to complete
kubectl rollout status deployment/ltr-ejb -n ltr-ejb --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed or timed out
    echo.
    echo Checking pod status...
    kubectl get pods -n ltr-ejb
    echo.
    echo Checking pod logs...
    kubectl logs -n ltr-ejb -l app=ltr-ejb --tail=50
    rmdir /S /Q "!TEMP_DIR!"
    exit /b 1
)

echo.
echo ==========================================
echo Deployment Verification
echo ==========================================
echo.

REM Display deployed resources
echo Pods:
kubectl get pods -n ltr-ejb -o wide

echo.
echo Services:
kubectl get svc -n ltr-ejb

echo.
echo Ingress:
kubectl get ingress -n ltr-ejb

echo.
echo ==========================================
echo Deployment Completed Successfully!
echo ==========================================
echo.

REM Get ingress URL
for /f "delims=" %%i in ('kubectl get ingress ltr-ejb-ingress -n ltr-ejb -o jsonpath^="{.status.loadBalancer.ingress[0].hostname}" 2^>nul') do set INGRESS_URL=%%i

if not "!INGRESS_URL!"=="" (
    echo Application URL: http://!INGRESS_URL!
) else (
    echo Ingress URL is being provisioned. Check status with:
    echo   kubectl get ingress ltr-ejb-ingress -n ltr-ejb
)

echo.
echo Useful commands:
echo   View pods:        kubectl get pods -n ltr-ejb
echo   View logs:        kubectl logs -n ltr-ejb -l app=ltr-ejb
echo   Describe pod:     kubectl describe pod ^<pod-name^> -n ltr-ejb
echo   Scale deployment: kubectl scale deployment ltr-ejb -n ltr-ejb --replicas=3
echo   Delete deployment: kubectl delete namespace ltr-ejb
echo.

REM Cleanup temp directory
rmdir /S /Q "!TEMP_DIR!"

endlocal
