pipeline {
    agent any  // 允许在任何可用节点上运行

   environment {
        PROJECT_ID = 'erp-prod-366106'  // GCP 项目 ID
        REGION = 'asia-east1'  // GCP 区域
        REPOSITORY = 'erp-image'  // 如果使用 GCR，可以忽略；Artifact Registry 仓库名称
        DOCKER_IMAGE = "${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}"  // 完整的 Docker 镜像路径
        DOCKER_TAG = "latest"  // Docker 镜像的标签
        GCP_CREDENTIALS = 'bc36fb28-b2ab-438d-9b7a-e75c0baf103b'  // Jenkins 中配置的 GCP 服务账号凭据 ID
    }

    stages {
        stage('Checkout') {  // 拉取代码
            steps {
                // 从 Git 仓库中检出代码
                git branch: 'main', url: 'https://github.com/thereisacompany/erp-api.git'
            }
        }

        stage('Build Docker Image') {  // 构建 Docker 镜像
            steps {
                script {
                    // 使用 Dockerfile 构建镜像
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                }
            }
        }

        stage('Test Docker Image') {  // 运行容器并测试
            steps {
                script {
                    // 运行一个临时容器，执行测试
                    sh "docker run --rm ${DOCKER_IMAGE}:${DOCKER_TAG} ./run_tests.sh"
                }
            }
        }

        stage('Push Docker Image to Registry') {  // 推送镜像到 Docker Registry
            steps {
                script {
                    // 使用凭据登录 Docker Registry
                    withCredentials([usernamePassword(credentialsId: "${REGISTRY_CREDENTIALS}", usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh 'echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin'
                        // 推送 Docker 镜像
                        sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    }
                }
            }
        }

        stage('Deploy') {  // 部署到服务器或容器
            steps {
                script {
                    echo 'Deploying to the server...'
                    // 在此处执行部署步骤，例如使用 ssh 或 docker-compose 部署
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
