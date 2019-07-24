#!groovy

@Library('visibilityLibs')
import com.liaison.jenkins.visibility.Utilities;
import com.liaison.jenkins.common.testreport.TestResultsUploader
import com.liaison.jenkins.common.servicenow.ServiceNow
import com.liaison.jenkins.common.slack.*

def serviceNow = new ServiceNow()
def slack = new Slack()
def utils = new Utilities();
def uploadUtil = new TestResultsUploader()

timestamps {
    def nexus_url = 'http://nexus.liaison.dev/content/repositories/dm-releases/'

    node {
        try {
            stage('Checkout') {
                timeout(10) {
                    checkout scm
                    env.VERSION = utils.runSh("awk '/^## \\[([0-9])/{ print (substr(\$2, 2, length(\$2) - 2));exit; }' CHANGELOG.md");
                    env.GIT_COMMIT = utils.runSh("git rev-parse HEAD");
                    env.GIT_URL = utils.runSh("git config remote.origin.url | sed -e 's/\\(.git\\)*\$//g' ")
                    env.REPO_NAME = utils.runSh("basename -s .git ${env.GIT_URL}")
                    currentBuild.displayName = 'v' + env.VERSION
                }
            }

            stage('Build') {
                timeout(20) {
                    sh "curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein"
                    sh "chmod a+x lein"
                    sh "./lein set-version ${env.VERSION}"
                    sh "./lein do set-version ${env.VERSION}"
                    sh "./lein do clean, install, jar, pom, uberjar"

                }
            }

            stage('Test') {
                timeout(15) {
                    sh "./lein test"

//                    if (utils.isMasterBuild()) {
//                        def unitTestInfo = uploadUtil.uploadResults(
//                                "${env.REPO_NAME}",
//                                "${env.VERSION}",
//                                "DEVINT",
//                                "proletariat Unit Test Results",
//                                "Unit test",
//                                "UNIT",
//                                ["./build/test-results/test/consolidated/proletariat_results-consolidated.xml"]
//                        )
//                        UT_REPORT_URL = unitTestInfo
//                    }
                }
            }

            stage('Check dependencies(TODO)') {
                timeout(10) {
                    echo "Dependency check not yet implemented!"
                }
            }

            if (!env.BRANCH_NAME.equals('master')) {
                // Fail PR build if version is not SNAPSHOT and it's already published to Nexus
                stage('Check published version') {
                    timeout(5) {
                        if (!env.VERSION.contains("SNAPSHOT")) {
                            PROLETARIAT_VERSION_EXISTS = sh(returnStdout: true, script: "curl -s -k -o /dev/null -I --head -w '%{http_code}' ${nexus_url}com/liaison/proletariat/${env.VERSION}/proletariat-${env.VERSION}.jar").trim() as Integer

                            if (PROLETARIAT_VERSION_EXISTS == 200) {
                                error "Proletariat ${version} already published to Nexus. Need to bump version number."
                            }
                        }
                    }
                }

                // Exit the node as this is not the master branch
                return
            }

            stage('Publish') {
                timeout(25) {
                    milestone()
                    withCredentials([file(credentialsId: 'maven-settings_nexus-liaison-dev',
                            variable: 'MAVEN_SETTINGS')])
                    {
                        // Install mvn
                        sh "curl -O https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.5.0/apache-maven-3.5.0-bin.tar.gz"
                        sh "tar xzvf apache-maven-3.5.0-bin.tar.gz"

                        sh "./apache-maven-3.5.0/bin/mvn -q --settings ${MAVEN_SETTINGS} deploy:deploy-file -Durl=${nexus_url} -DrepositoryId=dm-releases -DpomFile=pom.xml -Dfile=target/proletariat-${env.VERSION}.jar -Dfiles=target/proletariat-${env.VERSION}-standalone.jar -Dclassifiers=standalone -Dtypes=jar"

                        def releaseNotes = utils.runSh("awk '/## \\[${env.VERSION}\\]/{flag=1;next}/## \\[/{flag=0}flag' CHANGELOG.md")
                        utils.createGithubRelease("proletariat", env.GIT_COMMIT, env.VERSION, releaseNotes)
                    }
                    milestone()
                }
            }

        } catch (err) {
            currentBuild.result = "FAILURE";
            error "${err}"
        }
    }
}
