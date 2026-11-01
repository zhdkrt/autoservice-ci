pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat '"C:\\Users\\Admin\\apache-maven-3.9.9\\bin\\mvn.cmd" compile'
            }
        }

        stage('Static Analysis') {
            steps {
                bat '"C:\\Users\\Admin\\apache-maven-3.9.9\\bin\\mvn.cmd" checkstyle:check'
            }
        }

        stage('Test') {
            steps {
                bat '"C:\\Users\\Admin\\apache-maven-3.9.9\\bin\\mvn.cmd" test'
            }
        }

        stage('Package') {
            steps {
                bat '"C:\\Users\\Admin\\apache-maven-3.9.9\\bin\\mvn.cmd" package -DskipTests'
            }
        }

        stage('Inspect Target') {
            steps {
                echo 'Contents of target directory:'
                bat 'dir target'

                echo 'Created JAR files:'
                bat 'dir /b target\\*.jar'
            }
        }
    }
}