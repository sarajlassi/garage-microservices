#!/bin/bash

# =====================================================
# Jenkins Setup Script
# =====================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
JENKINS_URL=${JENKINS_URL:-"http://localhost:8080"}
JENKINS_USER=${JENKINS_USER:-"admin"}
JENKINS_TOKEN=${JENKINS_TOKEN}

# Functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_header "Jenkins Setup"

# Wait for Jenkins to be ready
echo "Waiting for Jenkins to be ready..."
for i in {1..60}; do
    if curl -s "$JENKINS_URL/login" > /dev/null; then
        print_success "Jenkins is ready"
        break
    fi
    if [ $i -eq 60 ]; then
        print_error "Jenkins is not responding after 60 seconds"
        exit 1
    fi
    echo "Waiting... ($i/60)"
    sleep 1
done

print_header "Installing Jenkins Plugins"

# List of plugins to install
PLUGINS=(
    "kubernetes:latest"
    "docker:latest"
    "pipeline-model-definition:latest"
    "pipeline-stage-view:latest"
    "Blue Ocean:latest"
    "sonar:latest"
    "cobertura:latest"
    "email-ext:latest"
    "git:latest"
    "github:latest"
    "credentials-binding:latest"
    "maven-plugin:latest"
)

for plugin in "${PLUGINS[@]}"; do
    echo "Installing plugin: $plugin"
    java -jar jenkins-cli.jar -s "$JENKINS_URL" install-plugin "$plugin" || true
done

print_success "Plugins installed"

# Restart Jenkins
echo "Restarting Jenkins..."
java -jar jenkins-cli.jar -s "$JENKINS_URL" restart || true
sleep 30

print_header "Creating Jenkins Job"

# Create pipeline job
cat << 'EOF' > /tmp/pipeline-job.xml
<?xml version='1.1' encoding='UTF-8'?>
<org.jenkinsci.plugins.workflow.job.WorkflowJob plugin="workflow-job@1306.v8e23937a_e3c5">
  <actions/>
  <description>CI/CD Pipeline for Garage Microservices</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2728.v3c8cecf3cfbc">
    <scm class="hudson.plugins.git.GitSCM" plugin="git@4.11.3">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>https://github.com/yourusername/garage-microservices.git</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>*/main</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="list"/>
      <extensions/>
    </scm>
    <scriptPath>Jenkinsfile</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</org.jenkinsci.plugins.workflow.job.WorkflowJob>
EOF

print_success "Job configuration created"

print_header "Jenkins Setup Completed"
echo ""
echo "Jenkins URL: $JENKINS_URL"
echo "Default admin user created"
echo ""
echo "Next steps:"
echo "1. Access Jenkins at $JENKINS_URL"
echo "2. Create initial admin user"
echo "3. Configure system settings"
echo "4. Create credentials for Docker and Kubernetes"
echo "5. Create pipeline job from Jenkinsfile"
echo ""

