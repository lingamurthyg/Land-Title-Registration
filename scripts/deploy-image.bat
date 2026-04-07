@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM Deploy to AWS EKS Script
REM Land Title Registry System
REM Platform: Windows
REM ============================================================================

echo ============================================================================
echo   Land Title Registry - AWS EKS Deployment Script
echo ============================================================================
echo.

REM Check prerequisites
echo Checking prerequisites...

where aws >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo ERROR: AWS CLI is not installed
    echo Please install AWS CLI: https://aws.amazon.com/cli/
    exit /b 1
)

where kubectl >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo ERROR: kubectl is not installed
    echo Please install kubectl: https://kubernetes.io/docs/tasks/tools/
    exit /b 1
)

echo [SUCCESS] Prerequisites check passed
echo.

REM Prompt for AWS configuration
echo AWS EKS Configuration
echo ----------------------------------------
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter EKS Cluster Name: "

echo.
echo AWS Region: !AWS_REGION!
echo EKS Cluster: !CLUSTER_NAME!
echo.

REM Configure kubectl
echo Configuring kubectl for EKS cluster...
aws eks update-kubeconfig --region "!AWS_REGION!" --name "!CLUSTER_NAME!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl
    exit /b 1
)

echo [SUCCESS] kubectl configured successfully
echo.

REM Verify cluster connectivity
echo Verifying cluster connectivity...
kubectl cluster-info

if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to cluster
    exit /b 1
)

echo [SUCCESS] Cluster connectivity verified
echo.

REM Prompt for Docker image URI
echo Docker Image Configuration
echo ----------------------------------------
set /p IMAGE_URI="Enter Docker Image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest): "

echo.
echo Image URI: !IMAGE_URI!
echo.

REM Prompt for database configuration
echo Database Configuration
echo ----------------------------------------
set /p DB_HOST="Enter Database Host: "
set /p DB_PORT="Enter Database Port [50000]: "
if "!DB_PORT!"=="" set DB_PORT=50000
set /p DB_NAME="Enter Database Name [landtitle]: "
if "!DB_NAME!"=="" set DB_NAME=landtitle
set /p DB_USER="Enter Database User: "
set /p DB_PASSWORD="Enter Database Password: "

echo.
echo Database Host: !DB_HOST!
echo Database Port: !DB_PORT!
echo Database Name: !DB_NAME!
echo Database User: !DB_USER!
echo.

REM Update Kubernetes manifests with actual values
echo Updating Kubernetes manifests...

REM Create temporary directory for processed manifests
set TEMP_DIR=%TEMP%\k8s-deploy-%RANDOM%
mkdir "!TEMP_DIR!"
xcopy /E /I /Q kubernetes "!TEMP_DIR!" >nul

REM Replace placeholders in deployment.yaml using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_USER}}', '!DB_USER!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo [SUCCESS] Manifests updated
echo.

REM Create Kubernetes secret for database password
echo Creating Kubernetes secret for database password...
kubectl create secret generic land-title-registry-secrets --from-literal=db-password="!DB_PASSWORD!" --namespace=land-title-registry --dry-run=client -o yaml | kubectl apply -f -

if !ERRORLEVEL! neq 0 (
    echo Warning: Failed to create secret (may already exist)
)

echo [SUCCESS] Secret created/updated
echo.

REM Deploy to Kubernetes
echo ============================================================================
echo Deploying to AWS EKS...
echo ============================================================================
echo.

REM Apply namespace
echo Creating namespace...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to create namespace
    exit /b 1
)
echo [SUCCESS] Namespace created
echo.

REM Apply deployment
echo Deploying application...
kubectl apply -f "!TEMP_DIR!\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to create deployment
    exit /b 1
)
echo [SUCCESS] Deployment created
echo.

REM Apply service
echo Creating service...
kubectl apply -f "!TEMP_DIR!\service.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to create service
    exit /b 1
)
echo [SUCCESS] Service created
echo.

REM Apply ingress
echo Creating ingress...
kubectl apply -f "!TEMP_DIR!\ingress.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to create ingress
    exit /b 1
)
echo [SUCCESS] Ingress created
echo.

REM Wait for deployment rollout
echo Waiting for deployment to complete...
kubectl rollout status deployment/land-title-registry -n land-title-registry --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed
    echo.
    echo Checking pod status:
    kubectl get pods -n land-title-registry
    echo.
    echo Checking pod logs:
    kubectl logs -n land-title-registry -l app=land-title-registry --tail=50
    exit /b 1
)

echo [SUCCESS] Deployment completed successfully
echo.

REM Verify deployment
echo ============================================================================
echo Verifying Deployment
echo ============================================================================
echo.

echo Pods:
kubectl get pods -n land-title-registry
echo.

echo Services:
kubectl get svc -n land-title-registry
echo.

echo Ingress:
kubectl get ingress -n land-title-registry
echo.

REM Get ingress URL
for /f "delims=" %%i in ('kubectl get ingress land-title-registry-ingress -n land-title-registry -o jsonpath^="{.status.loadBalancer.ingress[0].hostname}"') do set INGRESS_URL=%%i

if not "!INGRESS_URL!"=="" (
    echo Application URL: http://!INGRESS_URL!/ltr
    echo Health Check: http://!INGRESS_URL!/ltr/health
) else (
    echo Ingress URL not yet available. It may take a few minutes for the load balancer to be provisioned.
    echo Run the following command to check the ingress status:
    echo   kubectl get ingress land-title-registry-ingress -n land-title-registry
)

echo.
echo ============================================================================
echo Deployment Complete!
echo ============================================================================
echo.

REM Cleanup
rmdir /S /Q "!TEMP_DIR!"

echo Useful commands:
echo   View pods:        kubectl get pods -n land-title-registry
echo   View logs:        kubectl logs -n land-title-registry -l app=land-title-registry -f
echo   Describe pod:     kubectl describe pod ^<pod-name^> -n land-title-registry
echo   Scale deployment: kubectl scale deployment land-title-registry --replicas=3 -n land-title-registry
echo   Delete deployment: kubectl delete namespace land-title-registry
echo.

endlocal
