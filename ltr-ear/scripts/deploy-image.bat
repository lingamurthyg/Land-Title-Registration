@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM Deploy to AWS EKS - Land Title Registry EAR Application
REM Platform: Windows
REM Target: AWS EKS (Elastic Kubernetes Service)
REM ============================================================================

echo ============================================================================
echo   Land Title Registry - AWS EKS Deployment Script
echo ============================================================================
echo.

REM Prompt for AWS EKS configuration
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter EKS Cluster Name: "
set /p IMAGE_URI="Enter Docker Image URI (full path with tag): "

echo.
echo AWS Region: !AWS_REGION!
echo EKS Cluster: !CLUSTER_NAME!
echo Image URI: !IMAGE_URI!
echo.

REM Prompt for environment-specific configuration
echo --- Application Configuration ---
echo Enter values for environment variables (or press Enter to use defaults):
echo.

set /p DB_HOST="Database Host (default: database.example.com): "
if "!DB_HOST!"=="" set DB_HOST=database.example.com

set /p DB_PORT="Database Port (default: 5432): "
if "!DB_PORT!"=="" set DB_PORT=5432

set /p DB_NAME="Database Name (default: landtitledb): "
if "!DB_NAME!"=="" set DB_NAME=landtitledb

set /p DB_USER="Database User (default: ltruser): "
if "!DB_USER!"=="" set DB_USER=ltruser

set /p DB_PASSWORD="Database Password: "

set /p LDAP_HOST="LDAP Host (default: ldap.example.com): "
if "!LDAP_HOST!"=="" set LDAP_HOST=ldap.example.com

set /p LDAP_PORT="LDAP Port (default: 389): "
if "!LDAP_PORT!"=="" set LDAP_PORT=389

set /p LDAP_BASE_DN="LDAP Base DN (default: dc=example,dc=com): "
if "!LDAP_BASE_DN!"=="" set LDAP_BASE_DN=dc=example,dc=com

echo.
echo Configuration captured. Proceeding with deployment...
echo.

REM Configure kubectl for EKS
echo ============================================================================
echo Configuring kubectl for EKS cluster...
echo ============================================================================
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for EKS cluster
    exit /b 1
)

echo kubectl configured successfully
echo.

REM Verify cluster connectivity
echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to EKS cluster
    exit /b 1
)
echo.

REM Update Kubernetes manifests with configuration
echo ============================================================================
echo Updating Kubernetes manifests with configuration...
echo ============================================================================

REM Create temporary directory for processed manifests
set TEMP_DIR=%TEMP%\ltr-k8s-%RANDOM%
mkdir !TEMP_DIR!
xcopy /E /I /Q kubernetes !TEMP_DIR!

REM Replace placeholders in deployment.yaml using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_USER}}', '!DB_USER!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{LDAP_HOST}}', '!LDAP_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{LDAP_PORT}}', '!LDAP_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{LDAP_BASE_DN}}', '!LDAP_BASE_DN!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

REM Create database password secret if provided
if not "!DB_PASSWORD!"=="" (
    echo Creating database password secret...
    kubectl create secret generic ltr-db-secret --from-literal=password=!DB_PASSWORD! --namespace=ltr-ear --dry-run=client -o yaml | kubectl apply -f -
)

echo Manifests updated successfully
echo.

REM Apply Kubernetes manifests
echo ============================================================================
echo Deploying to AWS EKS...
echo ============================================================================

echo Creating namespace...
kubectl apply -f !TEMP_DIR!\namespace.yaml
echo.

echo Deploying application...
kubectl apply -f !TEMP_DIR!\deployment.yaml
echo.

echo Creating service...
kubectl apply -f !TEMP_DIR!\service.yaml
echo.

echo Creating ingress...
kubectl apply -f !TEMP_DIR!\ingress.yaml
echo.

REM Wait for deployment rollout
echo ============================================================================
echo Waiting for deployment to complete...
echo ============================================================================
kubectl rollout status deployment/ltr-ear -n ltr-ear --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo WARNING: Deployment rollout did not complete within timeout
    echo Check deployment status with: kubectl get pods -n ltr-ear
)

echo.

REM Verify deployment
echo ============================================================================
echo Verifying deployment...
echo ============================================================================
kubectl get pods,svc,ingress -n ltr-ear

echo.
echo ============================================================================
echo   Deployment Completed!
echo ============================================================================
echo.
echo Application Details:
echo   Namespace: ltr-ear
echo   Deployment: ltr-ear
echo   Service: ltr-ear-service
echo   Ingress: ltr-ear-ingress
echo.
echo To get the application URL:
echo   kubectl get ingress ltr-ear-ingress -n ltr-ear
echo.
echo To view logs:
echo   kubectl logs -f deployment/ltr-ear -n ltr-ear
echo.
echo To scale the deployment:
echo   kubectl scale deployment/ltr-ear --replicas=3 -n ltr-ear
echo.
echo To rollback if needed:
echo   kubectl rollout undo deployment/ltr-ear -n ltr-ear
echo.
echo ============================================================================

REM Cleanup temporary directory
rmdir /S /Q !TEMP_DIR!

endlocal
