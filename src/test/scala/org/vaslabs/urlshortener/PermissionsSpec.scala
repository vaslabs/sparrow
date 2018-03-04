package org.vaslabs.urlshortener

import org.scalatest.{FlatSpec, Matchers, WordSpec}
import org.vaslabs.urlshortener.permissions.PermissionMapping
import org.vaslabs.urlshortener.permissions.Permissions.{SpecialGuest, SuperUser, Unauthorised, WriteUser}
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._

class PermissionsSpec extends WordSpec with Matchers {

  "api keys mapping" should {
    "map super user" in {
      PermissionMapping("0000000000000000") shouldBe SuperUser
    }

    "map write user" in {
      PermissionMapping("0000000000000001") shouldBe WriteUser
    }

    "map special guest" in {
      PermissionMapping("0000000000000002") shouldBe SpecialGuest
    }

    "map everything else to unauthorised" in {
      PermissionMapping("0000000000000003") shouldBe Unauthorised
    }
  }
}
