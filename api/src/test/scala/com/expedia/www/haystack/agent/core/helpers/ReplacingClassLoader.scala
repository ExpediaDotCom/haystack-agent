package com.expedia.www.haystack.agent.core.helpers

import java.io.IOException
import java.net.URL
import java.util


/**
  * A ClassLoader to help test service providers.
  */
class ReplacingClassLoader(val parent: ClassLoader, val resource: String, val replacement: String) extends ClassLoader(parent) {
  override def getResource(name: String): URL = {
    if (resource == name) {
      return getParent.getResource(replacement)
    }
    super.getResource(name)
  }

  @throws[IOException]
  override def getResources(name: String): util.Enumeration[URL] = {
    if (resource == name) {
      return getParent.getResources(replacement)
    }
    super.getResources(name)
  }
}
