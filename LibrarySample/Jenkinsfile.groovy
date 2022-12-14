def slnFile = ""

pipeline {
    // Run on any available Jenkins agent.
    agent any
    options {
        // Show timestamps in the build.
        timestamps()
        // Prevent more than one build from running at a time for this project.
        disableConcurrentBuilds()
        // If Jenkins restarts or the client disconnects/reconnects, abandon the current build instead of trying to continue.
        disableResume()
    }
    triggers {
        // Poll source control periodically for changes.
        pollSCM 'H * * * *'
    }
    stages {
        stage('This is a new 6 test!') {
            steps {
                script {
                    print 'Hello good World!'


                          withCredentials([string(credentialsId: 'secret_test1', variable: 'SECRET')]) { //set SECRET with the credential content
        echo "My secret text is '${SECRET}'"
                            }


                            withCredentials([usernamePassword(credentialsId: 'password1', passwordVariable: 'pass', usernameVariable: 'user')]) {
    // the code here can access $pass and $user
     echo "My secret text is '${pass}'  and '${user}'"
}


                }
            }
        }

        stage('Find Solution') {
            steps {
                script {
                    // Search the repository for a file ending in .sln.
                    findFiles(glob: '**').each {
                        def path = it.toString();
                        echo "Found solution: ${path}"
                        if(path.toLowerCase().endsWith('.sln')) {
                            slnFile = path;
                        }
                    }
                    if(slnFile.length() == 0) {
                        throw new Exception('No solution files were found to build in the root of the git repository.')
                    }
                    echo "Found solution: ${slnFile}"
                }
            }
        }
        stage('Restore NuGet For Solution') {
            steps {
                // The command to restore includes:
                //  'NoCache' to avoid a shared cache--if multiple projects are running NuGet restore, they can collide.
                //  'NonInteractive' ensures no dialogs appear which can block builds from continuing.
                bat """
                    \"${tool 'NuGet-2019'}\" restore ${slnFile} -NoCache -NonInteractive
                    """
            }
        }
        stage('Build Solution') {
            steps {
                bat """
                    \"${tool 'MSBuild-2019'}\" ${slnFile} /p:Configuration=Release /p:Platform=\"Any CPU\" /p:ProductVersion=1.0.${env.BUILD_NUMBER}.0
                    """
            }
        }

stage ("Run Tests") {
     steps {
         script {

                     // Clean up any old test output from before so it doesn't contaminate this run.
            bat "IF EXIST TestResults rmdir /s /q TestResults"
 
            // The collection of tests to the work to do
            def tests = [:]

             // Find all the Test dlls that were built.
             def testAntPath = "**/bin/**/*.Tests.dll"
             findFiles(glob: testAntPath).each { f ->
                 String fullName = f
                  echo "Found tests: ${fullName}"

                                // Add a command to the map to run that test.
                tests["${fullName}"] = {
                    bat "\"${tool 'VSTest-2019'}\" /platform:x64 \"${fullName}\" /logger:trx /inIsolation /ResultsDirectory:TestResults"
                        }
                 }
                            // Runs the tests in parallel
            parallel tests
             }
         }
       }



       stage ("Convert Test Output") {
    steps {
        script {
            mstest testResultsFile:"TestResults/**/*.trx", failOnError: true, keepLongStdio: true
        }
    }
} 


    }
    post {
        always {
            junit 'build/reports/**/*.xml'
        }
    }
}