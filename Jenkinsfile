pipeline {
    agent any

    environment {
        // 💻 Docker
        DOCKER_IMAGE = 'matoapp-backend'
        DOCKER_TAG = "${BUILD_NUMBER}"

        // 🌐 공개 환경변수 (Jenkins Global Properties에 등록된 값)
        DB_HOST = 'mysql'
        DB_PORT = '3306'
        DB_NAME = 'mato'
        SERVER_PORT = '8081'
        SPRING_PROFILES_ACTIVE = 'prod'
        SPRING_DATA_REDIS_HOST = 'redis'
        SPRING_DATA_REDIS_PORT = '6379'
    }

    stages {
        stage('Load Secrets') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'db-credentials',
                        usernameVariable: 'DB_USERNAME',
                        passwordVariable: 'DB_PASSWORD'
                    ),
                    string(credentialsId: 'redis-password', variable: 'SPRING_DATA_REDIS_PASSWORD'),
                    string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET')
                ]) {
                    echo "✅ 비밀 환경변수 로딩 완료"
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build -x test'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    docker stop ${DOCKER_IMAGE} || true
                    docker rm ${DOCKER_IMAGE} || true
                    docker run -d \\
                        --name ${DOCKER_IMAGE} \\
                        -p ${SERVER_PORT}:${SERVER_PORT} \\
                        -e DB_HOST=${DB_HOST} \\
                        -e DB_PORT=${DB_PORT} \\
                        -e DB_NAME=${DB_NAME} \\
                        -e DB_USERNAME=${DB_USERNAME} \\
                        -e DB_PASSWORD=${DB_PASSWORD} \\
                        -e SPRING_DATA_REDIS_HOST=${SPRING_DATA_REDIS_HOST} \\
                        -e SPRING_DATA_REDIS_PORT=${SPRING_DATA_REDIS_PORT} \\
                        -e SPRING_DATA_REDIS_PASSWORD=${SPRING_DATA_REDIS_PASSWORD} \\
                        -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE} \\
                        -e JWT_SECRET=${JWT_SECRET} \\
                        -e SERVER_PORT=${SERVER_PORT} \\
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo '✅ 파이프라인이 성공적으로 완료되었습니다!'
        }
        failure {
            echo '❌ 파이프라인 실패. 로그 확인 필요.'
        }
    }
}
