package org.vaslabs.urlshortener.server

case class ShortenUrlRQ(url: String, customShortKey: Option[String])