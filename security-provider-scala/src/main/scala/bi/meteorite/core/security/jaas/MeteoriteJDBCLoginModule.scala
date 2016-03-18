/*
 * Copyright 2016 OSBI Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bi.meteorite.core.security.jaas

import java.io.IOException
import java.security.Principal
import java.sql.Connection
import java.util
import javax.security.auth.Subject
import javax.security.auth.callback._
import javax.security.auth.login.LoginException

import org.apache.karaf.jaas.boot.principal.{RolePrincipal, GroupPrincipal, UserPrincipal}
import org.apache.karaf.jaas.modules.{BackingEngine, AbstractKarafLoginModule}
import org.apache.karaf.jaas.modules.jdbc.JDBCUtils
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by brunogamacatao on 09/03/16.
  */
class MeteoriteJDBCLoginModule extends AbstractKarafLoginModule {
  var LOGGER = LoggerFactory.getLogger(classOf[MeteoriteJDBCLoginModule])

  var PASSWORD_QUERY = "query.password"
  var USER_QUERY = "query.user"
  var ROLE_QUERY = "query.role"
  var INSERT_USER_STATEMENT = "insert.user"
  var INSERT_ROLE_STATEMENT = "insert.role"
  var DELETE_ROLE_STATEMENT = "delete.role"
  var DELETE_ROLES_STATEMENT = "delete.roles"
  var DELETE_USER_STATEMENT = "delete.user"

  private var datasourceURL : String = null
  private var passwordQuery = "SELECT PASSWORD FROM USERS U, COMPANIES C WHERE U.USERNAME=? AND C.NAME=? AND U.COMPANY_ID = C.ID"
  private var roleQuery = "SELECT ROLENAME FROM ROLES R, USERS U, COMPANIES C WHERE U.USERNAME=? AND C.NAME=? AND R.USER_ID = U.ID AND U.COMPANY_ID = C.ID"

  override def initialize(subject: Subject,
                          callbackHandler: CallbackHandler,
                          sharedState: util.Map[String, _],
                          options: util.Map[String, _]): Unit = {
    super.initialize(subject, callbackHandler, options)

    datasourceURL = options.get(JDBCUtils.DATASOURCE).asInstanceOf[String]

    if (datasourceURL == null || datasourceURL.trim().length() == 0) {
      LOGGER.error("No datasource was specified ")
    } else if (!datasourceURL.startsWith(JDBCUtils.JNDI) && !datasourceURL.startsWith(JDBCUtils.OSGI)) {
      LOGGER.error("Invalid datasource lookup protocol")
    }

    if (options.containsKey(PASSWORD_QUERY)) {
      passwordQuery = options.get(PASSWORD_QUERY).asInstanceOf[String]
    }

    if (options.containsKey(ROLE_QUERY)) {
      roleQuery = options.get(ROLE_QUERY).asInstanceOf[String]
    }
  }

  override def login(): Boolean = {
    var connection: Connection = null

    try {
      connection = JDBCUtils.createDatasource(bundleContext, datasourceURL).getConnection()

      val callbacks = createCallbackArray()

      try {
        callbackHandler.handle(callbacks)
      } catch {
        case e: IOException => throw new LoginException("IOException: " + e.getMessage())
        case e: UnsupportedCallbackException => new LoginException(e.getMessage() + " not available to obtain information from user")
      }

      val company = getCompany(callbacks)
      user = callbacks(0).asInstanceOf[NameCallback].getName()
      val password = getPassword(callbacks)
      principals = new util.HashSet[Principal]()

      validatePassword(connection, company, user, password)
      loadPrincipals(connection, company, user)
    } finally {
      closeConnection(connection)
    }

    return true
  }

  override def logout(): Boolean = {
    subject.getPrincipals().removeAll(principals)
    principals.clear()
    if (debug) {
      LOGGER.debug("logout")
    }
    return true
  }

  override def abort(): Boolean = {
    return true
  }

  private def rawSelect(connection: Connection, query: String, params: String*) : Seq[String] = {
    val result = mutable.MutableList[String]()
    val statement = connection.prepareStatement(query)

    try {
      if (params != null) {
        for((param, index) <- params.view.zipWithIndex) {
          statement.setString(index + 1, param)
        }
      }

      val resultSet = statement.executeQuery()

      try {
        while (resultSet.next()) {
          result += resultSet.getString(1)
        }
      } finally {
        resultSet.close()
      }
    } finally {
      statement.close()
    }

    return result
  }

  private def createCallbackArray(): Array[Callback] = {
    val callbacks = new Array[Callback](2)

    callbacks(0) = new NameCallback("Username: ")
    callbacks(1) = new PasswordCallback("Password: ", false)

    return callbacks
  }

  private def getCompany(callbacks: Array[Callback]): String = {
      val nameCallback = callbacks(0).asInstanceOf[NameCallback]
      val companyNamePair = nameCallback.getName().split('\\')

      nameCallback.setName(companyNamePair(1))
      return companyNamePair(0)
  }

  private def getPassword(callbacks: Array[Callback]) : String = {
    var tmpPassword = callbacks(1).asInstanceOf[PasswordCallback].getPassword()

    if (tmpPassword == null) {
      tmpPassword = Array[Char]()
    }

    return tmpPassword.mkString
  }

  private def validatePassword(connection: Connection, company: String, user: String, password: String) : Unit = {
    val passwords = rawSelect(connection, passwordQuery, user, company)

    if (passwords.isEmpty) {
      throw new LoginException("User " + user + " does not exist")
    }

    if (!checkPassword(password, passwords(0))) {
      throw new LoginException("Password for " + user + " does not match");
    }
  }

  private def loadPrincipals(connection: Connection, company: String, user: String) : Unit = {
    principals.add(new UserPrincipal(user))

    val roles = rawSelect(connection, roleQuery, user, company)
    for (role <- roles) {
      if (role.startsWith(BackingEngine.GROUP_PREFIX)) {
        principals.add(new GroupPrincipal(role.substring(BackingEngine.GROUP_PREFIX.length())))
        for (r <- rawSelect(connection, roleQuery, role)) {
          principals.add(new RolePrincipal(r))
        }
      } else {
        principals.add(new RolePrincipal(role))
      }
    }
  }

  private def closeConnection(connection: Connection) : Unit = {
    if (connection != null) {
      try {
        connection.close()
      } catch {
        case e: Exception => throw new LoginException("Exception when closing connection: " + e.getMessage)
      }
    }
  }
}