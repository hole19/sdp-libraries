/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.git.steps

void call(Map args = [:], body){

  // do nothing if not merge
  if (!env.GIT_BUILD_CAUSE.equals("merge"))
    return

  def source_branch = get_source_branch()
  def target_branch = env.BRANCH_NAME

  // do nothing if source branch doesn't match
  if (args.from)
  if (!source_branch.collect{ it ==~ args.from}.contains(true))
    return

  // do nothing if target branch doesnt match
  if (args.to)
  if (!(target_branch ==~ args.to))
    return

  def mergedFrom = source_branch.join(", ")
  // grammar essentially, remove oxford comma to follow git syntax
  if(mergedFrom.contains(", ")) {
      def oxford = mergedFrom.lastIndexOf(", ")
      mergedFrom = mergedFrom.substring(0, oxford) + " and" + mergedFrom.substring(oxford + 1)
  }

  println "running because of a merge from ${mergedFrom} to ${target_branch}"
  body()
}

String get_source_branch(){
  node{
    unstash "workspace"

    env.FEATURE_SHA = get_feature_branch_sha()
    branch = get_merged_from()

    cleanWs()
    return branch
  }
}

String get_merged_from(){
  // update remote for git name-rev to properly work
  def remote = env.GIT_URL
  def cred_id = env.GIT_CREDENTIAL_ID
  withCredentials([usernamePassword(credentialsId: cred_id, passwordVariable: 'PASS', usernameVariable: 'USER')]){
      remote = remote.replaceFirst("://", "://${USER}:${PASS}@")
      sh "git remote rm origin"
      sh "git remote add origin ${remote}"
      sh "git fetch --all > /dev/null 2>&1"
  }
  // list all shas, but trim the first two shas
  // the first sha is the current commit
  // the second sha is the current commit's parent
  def sourceShas = sh(
    script: "git rev-list HEAD --parents -1",
    returnStdout: true
  ).trim().split(" ")[2..-1]
  def branchNames = []
  // loop through all shas and attempt to turn them into branch names
  for(sha in sourceShas) {
    def branch = run_script("git name-rev --name-only ${sha}").replaceFirst("remotes/origin/", "")
    // trim the ~<number> off branch names which means commits back
    // e.g. master~4 means 4 commits ago on master
    if(branch.contains("~"))
      branch = branch.substring(0, branch.lastIndexOf("~"))
    if(!branch.contains("^"))
      branchNames.add(branch)
  }
  // Didn't find any branch name, so last try is to check if can find it on the Merge message
  if (!branchNames){
    branchNames.add(get_branch_from_last_commit())
  }
  return branchNames
}

String get_feature_branch_sha(){
  run_script "git rev-parse \$(git --no-pager log -n1 | grep Merge: | awk '{print \$3}')"
}

String get_branch_from_last_commit(){
  run_script "git --no-pager log -1 | grep 'Merge pull' | awk '{print \$6}'"
}

String run_script(command){
  sh(
    script: command,
    returnStdout: true
  ).trim()
}
