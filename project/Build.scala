import sbt._
import Keys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep

object Build {
  def executeTask(task: TaskKey[_], info: String): State => State = (st: State) => {
    st.log.info(info)
    val extracted = Project.extract(st)
    val ref: ProjectRef = extracted.get(thisProjectRef)
    val (newState, _) = extracted.runTask(ref / task, st)
    newState
  }

// needs a rewrite after sbt upgrade
//  lazy val generateAndPushDocs: ReleaseStep = { st: State =>
//    val st2 = executeTask(ghpagesMakeSite, "Making doc site")(st)
//    executeTask(ghpagesPushSite, "Publishing doc site")(st2)
//  }
}
