pipeline {
  agent any
  environment {
    ACR_SERVER = "githubanalyzeracr.azurecr.io"
    RG         = "github-analyzer-rg"
  }
  stages {
    stage('Checkout')       { steps { checkout scm } }

    stage('Backend Tests')  { steps { sh 'mvn -B test -f backend/pom.xml' } }

    stage('Build Images') {
      steps {
        sh 'docker build -t $ACR_SERVER/gh-analyzer-api:$BUILD_NUMBER ./backend'
        sh 'docker build -t $ACR_SERVER/gh-analyzer-ui:$BUILD_NUMBER ./frontend'
      }
    }

    stage('Push to ACR') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'acr-creds',
            usernameVariable: 'ACR_USER', passwordVariable: 'ACR_PASS')]) {
          sh 'echo $ACR_PASS | docker login $ACR_SERVER -u $ACR_USER --password-stdin'
          sh 'docker push $ACR_SERVER/gh-analyzer-api:$BUILD_NUMBER'
          sh 'docker push $ACR_SERVER/gh-analyzer-ui:$BUILD_NUMBER'
        }
      }
    }

    stage('Deploy to Azure') {
      steps {
        withCredentials([azureServicePrincipal('azure-sp')]) {
          sh '''
            az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID
            az webapp config container set --name github-analyzer-api-niksoni --resource-group $RG \
               --container-image-name $ACR_SERVER/gh-analyzer-api:$BUILD_NUMBER
            az webapp config container set --name github-analyzer-ui-niksoni --resource-group $RG \
               --container-image-name $ACR_SERVER/gh-analyzer-ui:$BUILD_NUMBER
          '''
        }
      }
    }
  }
}