@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM Deploy Land Title Registry Web Application to AWS EKS
REM ============================================================================
REM This script deploys the containerized application to AWS EKS cluster
REM Prerequisites: AWS CLI, kubectl, valid AWS credentials
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
echo --- Optional: Database Configuration ---
echo Press Enter to skip if using Kubernetes secrets
set /p DB_HOST="Enter Database Host (or press Enter to skip): "
set /p DB_PORT="Enter Database Port (or press Enter to skip): "
set /p DB_NAME="Enter Database Name (or press Enter to skip): "
set /p DB_USER="Enter Database User (or press Enter to skip): "

REM Set defaults if not provided
if "!DB_HOST!"=="" set DB_HOST=localhost
if "!DB_PORT!"=="" set DB_PORT=50000
if "!DB_NAME!"=="" set DB_NAME=LTRDB
if "!DB_USER!"=="" set DB_USER=db2admin

echo.
echo ============================================================================
echo Configuring kubectl for EKS
echo ============================================================================
echo Region: !AWS_REGION!
echo Cluster: !CLUSTER_NAME!
echo.

aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for EKS cluster
    exit /b 1
)

echo.
echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to Kubernetes cluster
    exit /b 1
)

echo.
echo ============================================================================
echo Updating Kubernetes Manifests
echo ============================================================================
echo.

REM Update deployment.yaml with image URI and environment variables
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_USER}}', '!DB_USER!' | Set-Content kubernetes\deployment.yaml"

echo Manifests updated successfully

echo.
echo ============================================================================
echo Deploying to AWS EKS
echo ============================================================================
echo.

REM Apply Kubernetes manifests in order
echo Creating namespace...
kubectl apply -f kubernetes\namespace.yaml

echo.
echo Deploying application...
kubectl apply -f kubernetes\deployment.yaml

echo.
echo Creating service...
kubectl apply -f kubernetes\service.yaml

echo.
echo Creating ingress...
kubectl apply -f kubernetes\ingress.yaml

echo.
echo ============================================================================
echo Waiting for Deployment Rollout
echo ============================================================================
echo.

kubectl rollout status deployment/ltr-web -n ltr-web --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo WARNING: Deployment rollout did not complete within timeout
    echo Check pod status with: kubectl get pods -n ltr-web
)

echo.
echo ============================================================================
echo Deployment Status
echo ============================================================================
echo.

kubectl get pods,svc,ingress -n ltr-web

echo.
echo ============================================================================
echo Deployment Completed
echo ============================================================================
echo.
echo Application deployed to namespace: ltr-web
echo Image: !IMAGE_URI!
echo.
echo To check application logs:
echo   kubectl logs -f deployment/ltr-web -n ltr-web
echo.
echo To get ingress URL:
echo   kubectl get ingress ltr-web-ingress -n ltr-web -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"
echo.
echo To scale deployment:
echo   kubectl scale deployment/ltr-web -n ltr-web --replicas=3
echo.
echo To rollback deployment:
echo   kubectl rollout undo deployment/ltr-web -n ltr-web
echo.
echo ============================================================================

endlocal
