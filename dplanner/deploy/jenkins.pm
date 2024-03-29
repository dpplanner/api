pipeline {
    agent any

    stages {
        stage('Git Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/dpplanner/api.git'
            }
        }

        stage('oauth.yml Download') {
            steps {
                withCredentials([file(credentialsId: 'oauth-credentials', variable: 'oauthConfig')]) {
                    script {

                        echo 'Current user: ' + sh(script: 'whoami', returnStdout: true).trim()
                        echo 'Current directory: ' + sh(script: 'pwd', returnStdout: true).trim()

                        // Adjust permissions of destination directory

                        sh 'chmod -R +w dplanner/src/main/resources/'
                        sh 'chown -R jenkins dplanner/src/main/resources/'


                        sh 'cp $oauthConfig dplanner/src/main/resources/application-oauth.yml'
                    }
                }
            }
        }

        stage('firebase sdk Download') {
            steps {
                withCredentials([file(credentialsId: 'firebase-adminsdk', variable: 'firebaseSDK')]) {
                    script {

                        echo 'Current user: ' + sh(script: 'whoami', returnStdout: true).trim()
                        echo 'Current directory: ' + sh(script: 'pwd', returnStdout: true).trim()

                        // Adjust permissions of destination directory

                        sh 'chmod -R +w dplanner/src/main/resources/'
                        sh 'chown -R jenkins dplanner/src/main/resources/'


                        sh 'cp $firebaseSDK dplanner/src/main/resources/dplanner-c2b9a-firebase-adminsdk-drga5-5c3beeb1bb.json'
                    }
                }
            }
        }

        stage('Environment Variable Substitute') {
            steps {
                script {
                    echo 'Current user: ' + sh(script: 'whoami', returnStdout: true).trim()
                    echo 'Current directory: ' + sh(script: 'pwd', returnStdout: true).trim()

                    sh 'sed -i "s/\\${DB_USER}/${DB_USER}/" ./dplanner/src/main/resources/application-production.yml'
                    sh 'sed -i "s/\\${DB_PASSWORD}/${DB_PASSWORD}/" ./dplanner/src/main/resources/application-production.yml'
                    sh 'sed -i "s/\\${AWS_S3_ACCESSKEY}/${AWS_S3_ACCESSKEY}/" ./dplanner/src/main/resources/application-production.yml'
                    sh 'sed -i "s/\\${AWS_S3_SECRETKEY}/${AWS_S3_SECRETKEY}/" ./dplanner/src/main/resources/application-production.yml'
                    sh 'sed -i "s#\\${DB_URL}#${DB_URL}#" ./dplanner/src/main/resources/application-production.yml'
                }
            }
        }

        stage('Build Gradle') {
            steps {
                dir('./dplanner') {
                    sh 'chmod +x ./gradlew'
                    sh './gradlew clean build'
                }
            }
        }

        stage('Deploy') {
            steps {
                sshagent(credentials:['aws_key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no ubuntu@3.39.102.31 uptime
                        scp /var/jenkins_home/workspace/dplanner-CICD@/dplanner/build/libs/dplanner-0.0.1-SNAPSHOT.jar ubuntu@3.39.102.31:/home/ubuntu/dplanner
                        ssh -t ubuntu@3.39.102.31 ./deploy.sh
                    '''
                }
            }
        }
    }
}
