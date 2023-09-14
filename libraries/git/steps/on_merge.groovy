/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.git.steps

void call(Map args = [:], body){

  // do nothing if not merge
  if (!env.GIT_BUILD_CAUSE.equals("merge"))
    return

  def source_branch = env.SOURCE_BRANCHES.split(",")
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
