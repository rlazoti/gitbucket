package servlet

import com.mchange.v2.c3p0.ComboPooledDataSource
import javax.servlet._
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest
import slick.jdbc.JdbcBackend.{Database => SlickDatabase}
import slick.jdbc.JdbcBackend.Session
import util.Keys
import util.Directory._

/**
 * Controls the transaction with the open session in view pattern.
 */
class TransactionFilter extends Filter {

  private val logger = LoggerFactory.getLogger(classOf[TransactionFilter])

  def init(config: FilterConfig) = {}

  def destroy(): Unit = {}

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    if(req.asInstanceOf[HttpServletRequest].getRequestURI().startsWith("/assets/")){
      // assets don't need transaction
      chain.doFilter(req, res)
    } else {
      Database() withTransaction { session =>
        logger.debug("begin transaction")
        req.setAttribute(Keys.Request.DBSession, session)
        chain.doFilter(req, res)
        logger.debug("end transaction")
      }
    }
  }

}

object Database {

  private val logger = LoggerFactory.getLogger(Database.getClass)

  private val db: SlickDatabase = {
    val datasource = new ComboPooledDataSource

    datasource.setDriverClass("org.h2.Driver")
    datasource.setJdbcUrl(s"jdbc:h2:${DatabaseHome};MVCC=true")
    datasource.setUser("sa")
    datasource.setPassword("sa")

    logger.debug("load database connection pool")

    SlickDatabase.forDataSource(datasource)
  }

  def apply(): SlickDatabase = db

  def getSession(req: ServletRequest): Session =
    req.getAttribute(Keys.Request.DBSession).asInstanceOf[Session]

}
