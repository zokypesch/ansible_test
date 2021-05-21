pipeline {
  environment {
    registry = "prakerja/${params.projects}"
    registryCredential = 'cr-slave'
    dockerImage = ''
    tag = ""
    privateKey = ''
  }
  agent any
  stages {
    stage('Cloning Git') {
      steps {
        git branch: "master",
            credentialsId: 'devops_gitlab',
            url: "https://gitlab.com/prakerja/${params.projects}.git"
      }
    }
    stage('get commit id') {
        steps {
            script {
                sh "git rev-parse --short HEAD > commit-id"
                tag = readFile('commit-id').replace('\n', '').replace('\r', '')   
            }
        }
    }
    stage('assign key') {
      steps{
        script {
        
          privateKey = readFile '/var/jenkins_home/.ssh/id_rsa'
          
        }
      }
    }
    stage('Building image') {
      steps{
        script {
          dockerImage = docker.build(registry+":$tag", "--build-arg SSH_PRIVATE_KEY='$privateKey' .")
        }
      }
    }
    stage('Push Image') {
      steps{
        script {
          docker.withRegistry( 'https://registry-intl.ap-southeast-5.aliyuncs.com/'+"$registry" , registryCredential ) {
            dockerImage.push()
          }
        }
      }
    }
    stage('Running Ansible') {
      steps {
        //   start
        script {
                sshPublisher(
                    continueOnError: false, failOnError: true,
                    publishers: [
                        sshPublisherDesc(
                            configName: "ansible-1",
                            verbose: true,
                            transfers: [
                                sshTransfer(
                                    execCommand: 'cd /root/controller_deployment_landing && ./slb_controller_landing -mode=rollout -id=i-k1a5jk5dipulwzzpggn1'
                                ),
                                sshTransfer(
                                    execCommand: 'cd /ansible_test/playbooks && ansible-playbook -i ../inventori/main.ini -e "host=' + "frontend-server-1" +'" -e "digest='+"$tag"+'" landing.yaml'
                                ),
                                 sshTransfer(
                                    execCommand: 'cd /root/controller_deployment_landing && ./slb_controller_landing -mode=deploy -id=i-k1a5jk5dipulwzzpggn1'
                                ),
                                sshTransfer(
                                    execCommand: 'cd /root/controller_deployment_landing && ./slb_controller_landing -mode=rollout -id=i-k1a5jk5dipulx1yqkim7'
                                ),
                                sshTransfer(
                                    execCommand: 'cd /ansible_test/playbooks && ansible-playbook -i ../inventori/main.ini -e "host=' + "frontend-server-2" +'" -e "digest='+"$tag"+'" landing.yaml'
                                ),
                                sshTransfer(
                                    execCommand: 'cd /root/controller_deployment_landing && ./slb_controller_landing -mode=deploy -id=i-k1a5jk5dipulx1yqkim7'
                                )
                            ]
                        )
                    ]
                )
            }
        }
        // end
      }
    }
}
