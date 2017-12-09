package org.vaslabs.urlshortener

trait PersistentStore {

  def fetch(id: String): ShortenedUrl
}
