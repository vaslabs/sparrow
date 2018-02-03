package org.vaslabs.urlshortener.permissions

import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{Less, Positive}
import eu.timepit.refined._
import eu.timepit.refined.auto._

object Permissions {

  sealed trait User

  sealed trait IdSize {
    val urlIdMinSize: Int Refined Positive
  }


  sealed trait CanCreateNew extends IdSize

  sealed trait CanAliasUrls extends CanCreateNew


  private[permissions] final val SUPER_USER_KEY_NAME = "X_SPARROW_SU_AUTH"

  private[permissions] final val WRITE_USER = "X_SPARROW_AUTH"

  private[permissions] final val SPECIAL_GUEST = "X_SPARROW_AUTH_SPECIAL"


  final object SuperUser extends User with CanAliasUrls {
    override val urlIdMinSize = 4
  }

  final object WriteUser extends User with CanCreateNew {
    override val urlIdMinSize = 6
  }

  final object SpecialGuest extends User with CanCreateNew {
    override val urlIdMinSize = 4
  }

  final object Unauthorised extends User

}

object PermissionMapping {
  import Permissions._
  import eu.timepit.refined.string._


  private[this] lazy val superUserAuthKey = sys.env.getOrElse(SUPER_USER_KEY_NAME, "0000000000000000")

  private[this] lazy val writeUser = sys.env.getOrElse(WRITE_USER, "0000000000000001")
  private[this] lazy val specialGuest = sys.env.getOrElse(SPECIAL_GUEST, "0000000000000002")

  private type API_KEY = String Refined API_REGEX

  type API_REGEX = MatchesRegex[W.`"[a-f0-9]{16}"`.T]

  private[this] lazy val userMapping = Map[String, User](
    superUserAuthKey -> SuperUser,
    writeUser -> WriteUser,
    specialGuest -> SpecialGuest
  )

  def apply(apiKey: API_KEY): User =
    userMapping.get(apiKey.value).getOrElse(Unauthorised)

}
