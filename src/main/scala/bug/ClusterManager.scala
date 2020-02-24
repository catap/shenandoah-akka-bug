package bug

import akka.actor._
import akka.cluster._
import akka.management.cluster.bootstrap.ClusterBootstrap

import scala.language.postfixOps

object ClusterManager {
  def restartClusterManager: Unit =
    new ClusterManager
}

class ClusterManager {
  private val system = ActorSystem("System")

  private val cluster = Cluster(system)

  ClusterBootstrap(system).start()
}
