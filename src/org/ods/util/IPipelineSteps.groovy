package org.ods.util

@SuppressWarnings('MethodCount')
interface IPipelineSteps {

    void archiveArtifacts(String artifacts)

    def checkout(Map config)

    def dir(String path, Closure block)

    void echo(String message)

    def getCurrentBuild()

    Map getEnv()

    void junit(String path)

    void junit(Map config)

    def load(String path)

    def sh(def args)

    def bat(def args)

    void stage(String name, Closure block)

    void stash(String name)

    void stash(Map config)

    void unstash(String name)

    def fileExists(String file)

    def readFile(String file, String encoding)

    def readFile(Map args)

    def writeFile(String file, String text, String encoding)

    def writeFile(Map args)

    def readJSON(Map args)

    def writeJSON(Map args)

    def timeout(Map args, Closure block)

    def deleteDir()

    def sleep(int seconds)

    def withEnv(List<String> env, Closure block)

    def unstable(String message)

    def usernamePassword(Map credentialsData)

    def sshUserPrivateKey(Map credentialsData)

    def withCredentials(List credentialsList, Closure block)

    def unwrap()

    def zip(Map args)

    def unzip(Map args)

    def findFiles(Map args)

    def isUnix()

}
