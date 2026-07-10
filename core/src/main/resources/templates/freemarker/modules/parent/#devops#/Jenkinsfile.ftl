pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    MAVEN_OPTS = '-Dfile.encoding=UTF-8'
  }

  stages {
    stage('Build') {
      steps {
        sh '''
          if [ -x ./package.sh ]; then
            ./package.sh --no-clean
          else
            mvn -B -ntp -DskipTests package
          fi
        '''
      }
    }

    stage('Test') {
      steps {
        sh '''
          if command -v mvn >/dev/null 2>&1; then
            mvn -B -ntp test
          else
            echo "mvn not found, skip test stage"
          fi
        '''
      }
    }

    stage('SonarQube') {
      when {
        expression {
          return env.SONAR_HOST_URL?.trim() && env.SONAR_TOKEN?.trim()
        }
      }
      steps {
        sh 'mvn -B -ntp sonar:sonar -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONAR_TOKEN'
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
      junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
    }
  }
}
