import sbt._
import Keys._
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep

object Build extends Build {
  def executeTask(task: TaskKey[_], info: String): State => State = (st: State) => {
    st.log.info(info)
    val extracted = Project.extract(st)
    val ref: ProjectRef = extracted.get(thisProjectRef)
    val (newState, _) = extracted.runTask(task in ref, st)
    newState
  }

  lazy val generateAndPushDocs: ReleaseStep = { st: State =>
    val st2 = executeTask(makeSite, "Making doc site")(st)
    executeTask(pushSite, "Publishing doc site")(st2)
  }
}
